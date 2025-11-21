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

$mid = isset($data['message_id']) ? (int)$data['message_id'] : 0;
if ($mid <= 0) json_response(["success"=>false,"message"=>"message_id required"]);

$q = $conn->prepare("SELECT sender_id, created_at FROM messages WHERE id = ? LIMIT 1");
$q->bind_param("i", $mid); $q->execute(); $q->store_result();
if ($q->num_rows === 0) json_response(["success"=>false,"message"=>"Message not found"]);
$q->bind_result($sender_id, $created_at); $q->fetch(); $q->close();

if ((int)$sender_id !== (int)$user_id) json_response(["success"=>false,"message"=>"Only sender can delete message"]);

$created_ts = strtotime($created_at);
if (time() - $created_ts > 300) json_response(["success"=>false,"message"=>"Deletion window expired (5 minutes)"], 403);

$upd = $conn->prepare("UPDATE messages SET is_deleted = 1 WHERE id = ?");
$upd->bind_param("i", $mid); $ok = $upd->execute();
if (!$ok) json_response(["success"=>false,"message"=>"Failed to delete","error"=>$conn->error]);

json_response(["success"=>true, "deleted"=>$mid]);
?>
