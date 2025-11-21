<?php
require "db.php";
require "helpers.php";

// Called by client when user closes a chat UI. Server will remove messages
// in the conversation that have vanish_on_close=1 and have been seen already.

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) $data = $_POST;

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token); $stmt->execute(); $stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid or expired token"]);
$stmt->bind_result($user_id);
$stmt->fetch();
$stmt->close();

$conv_id = isset($data['conversation_id']) ? (int)$data['conversation_id'] : 0;
if ($conv_id <= 0) json_response(["success"=>false,"message"=>"conversation_id required"]);

// Start a transaction to ensure atomicity
$conn->begin_transaction();

try {
    // Delete the messages marked for vanishing
    $del = $conn->prepare("DELETE FROM messages WHERE conversation_id = ? AND vanish_on_close = 1 AND is_seen = 1");
    $del->bind_param("i", $conv_id);
    $del->execute();
    $deleted_count = $del->affected_rows;
    $del->close();

    // After deleting, find the new latest message in the conversation
    $new_last_message_id = null;
    $stmt_new_last = $conn->prepare("SELECT id FROM messages WHERE conversation_id = ? ORDER BY created_at DESC, id DESC LIMIT 1");
    $stmt_new_last->bind_param("i", $conv_id);
    $stmt_new_last->execute();
    $stmt_new_last->bind_result($new_last_message_id);
    $stmt_new_last->fetch();
    $stmt_new_last->close();

    // Update the conversations table with the new last_message_id
    $stmt_update_conv = $conn->prepare("UPDATE conversations SET last_message_id = ? WHERE id = ?");
    $stmt_update_conv->bind_param("ii", $new_last_message_id, $conv_id);
    $stmt_update_conv->execute();
    $stmt_update_conv->close();

    // Commit the transaction
    $conn->commit();

    json_response(["success"=>true, "deleted"=>$deleted_count]);

} catch (Exception $e) {
    $conn->rollback();
    error_log("vanish_on_close.php error: " . $e->getMessage());
    json_response(["success"=>false, "message"=>"Server error during vanish operation"]);
}
?>
