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

$status = isset($data['status']) && $data['status'] === 'online' ? 'online' : 'offline';

$sql = "INSERT INTO user_presence (user_id, status, last_seen) VALUES (?, ?, NULL) ON DUPLICATE KEY UPDATE status = VALUES(status), updated_at = NOW(), last_seen = (CASE WHEN VALUES(status)='offline' THEN NOW() ELSE last_seen END)";
$st = $conn->prepare($sql);
$st->bind_param("is", $user_id, $status); $ok = $st->execute();
if (!$ok) json_response(["success"=>false,"message"=>"Failed to set status","error"=>$conn->error]);
$st->close();

json_response(["success"=>true, "status"=>$status]);
?>
