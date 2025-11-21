<?php
require "db.php";
require "helpers.php";
require "fcm_helper.php"; // Include our new FCM helper

// Accept JSON body or form-data
$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) $data = $_POST;

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id, (SELECT username FROM users WHERE id = user_id) as username FROM tokens WHERE token=? LIMIT 1");
$stmt->bind_param("s", $token); $stmt->execute(); $res = $stmt->get_result();
if ($res->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid token"]);
$caller = $res->fetch_assoc();
$user_id = $caller['user_id'];
$sender_name = $caller['username'];
$stmt->close();

$receiver_id = isset($data['receiver_id']) ? (int)$data['receiver_id'] : 0;
$conversation_id = isset($data['conversation_id']) ? (int)$data['conversation_id'] : 0;
$content = isset($data['content']) ? trim($data['content']) : null;
$type = isset($data['type']) ? $data['type'] : 'text';
$attachment_url = isset($data['attachment_url']) ? $data['attachment_url'] : null;
$vanish = isset($data['vanish_on_close']) && $data['vanish_on_close'] ? 1 : 0;

if ($conversation_id <= 0 && $receiver_id <= 0) json_response(["success"=>false,"message"=>"receiver_id or conversation_id required"]);

// ... (rest of your existing validation logic)

// find or create conversation
$conn->begin_transaction();
try {
    if ($conversation_id > 0) {
        $conv_id = $conversation_id;
    } else {
        $a = min($user_id, $receiver_id);
        $b = max($user_id, $receiver_id);
        $q = $conn->prepare("SELECT id FROM conversations WHERE user_a = ? AND user_b = ? LIMIT 1");
        $q->bind_param("ii", $a, $b); $q->execute(); $q->store_result();
        if ($q->num_rows > 0) { $q->bind_result($conv_id); $q->fetch(); }
        else {
            $ins = $conn->prepare("INSERT INTO conversations (user_a, user_b) VALUES (?, ?)");
            $ins->bind_param("ii", $a, $b); $ins->execute(); $conv_id = $ins->insert_id; $ins->close();
        }
        $q->close();
    }
} catch (Exception $e) {
    $conn->rollback();
    json_response(["success"=>false,"message"=>"Failed to resolve conversation","error"=>$e->getMessage()]);
}

// insert message
try {
    $insm = $conn->prepare("INSERT INTO messages (conversation_id, sender_id, receiver_id, content, type, attachment_url, vanish_on_close) VALUES (?, ?, ?, ?, ?, ?, ?)");
    $insm->bind_param("iiisssi", $conv_id, $user_id, $receiver_id, $content, $type, $attachment_url, $vanish);
    if (!$insm->execute()) throw new Exception('Message insert failed: ' . $conn->error);
    $message_id = $insm->insert_id; $insm->close();

    $upd = $conn->prepare("UPDATE conversations SET last_message_id = ? WHERE id = ?");
    $upd->bind_param("ii", $message_id, $conv_id); $upd->execute(); $upd->close();

    $conn->commit();

    // --- SEND NOTIFICATION ---
    $fcm_stmt = $conn->prepare("SELECT fcm_token FROM fcm_tokens WHERE user_id = ?");
    $fcm_stmt->bind_param("i", $receiver_id);
    $fcm_stmt->execute();
    $fcm_stmt->bind_result($fcm_token);
    $fcm_stmt->fetch();
    $fcm_stmt->close();

    if (!empty($fcm_token)) {
        $notification_body = ($type === 'image') ? 'Sent you an image.' : $content;
        $notification_data = [
            "type" => "new_message",
            "title" => "New Message from $sender_name",
            "body" => $notification_body,
            "sender_id" => (string)$user_id,
            "conversation_id" => (string)$conv_id
        ];
        send_fcm_notification($fcm_token, $notification_data);
    }
    // --- END NOTIFICATION ---

    $stmtm = $conn->prepare("SELECT * FROM messages WHERE id = ? LIMIT 1");
    $stmtm->bind_param("i", $message_id); $stmtm->execute(); $res = $stmtm->get_result(); $m = $res->fetch_assoc(); $stmtm->close();

    json_response(["success"=>true, "conversation_id"=>$conv_id, "message"=>$m]);

} catch (Exception $e) {
    $conn->rollback();
    json_response(["success"=>false,"message"=>"Failed to send message","error"=>$e->getMessage()]);
}
?>
