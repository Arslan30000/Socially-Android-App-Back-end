<?php
require "db.php";
require "helpers.php";

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) $data = $_POST;

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token); $stmt->execute(); $stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid or expired token"]);
$stmt->bind_result($user_id); $stmt->fetch();

$conv = isset($data['conversation_id']) ? (int)$data['conversation_id'] : 0;
$ids = isset($data['message_ids']) ? $data['message_ids'] : null;

if ($conv <= 0) json_response(["success"=>false,"message"=>"conversation_id required"]);

if ($ids && is_array($ids) && count($ids) > 0) {
    // prepare dynamic placeholders
    $placeholders = implode(',', array_fill(0, count($ids), '?'));
    $types = str_repeat('i', count($ids));
    $sql = "UPDATE messages SET is_seen = 1 WHERE conversation_id = ? AND receiver_id = ? AND id IN ($placeholders)";
    $stmt2 = $conn->prepare($sql);
    $params = array_merge([$conv, $user_id], $ids);
    $refs = [];
    $types_all = 'ii' . $types;
    $refs[] = & $types_all;
    foreach ($params as $k => $v) { $refs[] = & $params[$k]; }
    call_user_func_array(array($stmt2, 'bind_param'), $refs);
    $stmt2->execute();
    $affected = $stmt2->affected_rows; $stmt2->close();
} else {
    $stmt2 = $conn->prepare("UPDATE messages SET is_seen = 1 WHERE conversation_id = ? AND receiver_id = ? AND is_seen = 0");
    $stmt2->bind_param("ii", $conv, $user_id); $stmt2->execute(); $affected = $stmt2->affected_rows; $stmt2->close();
}

json_response(["success"=>true, "marked"=>(int)$affected]);
?>
