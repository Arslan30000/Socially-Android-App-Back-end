<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token); $stmt->execute(); $stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid or expired token"]);
$stmt->bind_result($user_id); $stmt->fetch();

$conv = isset($_GET['conversation_id']) ? (int)$_GET['conversation_id'] : 0;
$limit = isset($_GET['limit']) ? max(10, min(200, (int)$_GET['limit'])) : 50;
$before_id = isset($_GET['before_id']) ? (int)$_GET['before_id'] : 0;
if ($conv <= 0) json_response(["success"=>false,"message"=>"conversation_id required"]);

if ($before_id > 0) {
    $q = $conn->prepare("SELECT id, conversation_id, sender_id, receiver_id, content, type, attachment_url, is_seen, is_deleted, is_edited, vanish_on_close, created_at FROM messages WHERE conversation_id = ? AND id < ? AND is_deleted = 0 ORDER BY id DESC LIMIT ?");
    $q->bind_param("iii", $conv, $before_id, $limit);
} else {
    $q = $conn->prepare("SELECT id, conversation_id, sender_id, receiver_id, content, type, attachment_url, is_seen, is_deleted, is_edited, vanish_on_close, created_at FROM messages WHERE conversation_id = ? AND is_deleted = 0 ORDER BY id DESC LIMIT ?");
    $q->bind_param("ii", $conv, $limit);
}
$q->execute(); $res = $q->get_result();
$messages = [];
while ($r = $res->fetch_assoc()) {
    $messages[] = $r;
}

// messages were fetched in DESC order; reverse so client gets ASC
$messages = array_reverse($messages);

json_response(["success"=>true, "messages"=>$messages]);
?>
