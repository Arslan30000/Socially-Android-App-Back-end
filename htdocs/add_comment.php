<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success"=>false, "message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false, "message"=>"Invalid or expired token"]);
$stmt->bind_result($user_id);
$stmt->fetch();

$post_id = isset($_POST['post_id']) ? intval($_POST['post_id']) : 0;
$text = isset($_POST['text']) ? trim($_POST['text']) : '';
if ($post_id <= 0 || $text === '') json_response(["success"=>false, "message"=>"post_id and text are required"]);

// Ensure post exists
$p = $conn->prepare("SELECT id FROM posts WHERE id=? LIMIT 1");
$p->bind_param('i', $post_id);
$p->execute();
$p->store_result();
if ($p->num_rows === 0) json_response(["success"=>false, "message"=>"Post not found"]);

$ins = $conn->prepare("INSERT INTO comments (post_id, user_id, text, timestamp) VALUES (?, ?, ?, NOW())");
if (!$ins) json_response(["success"=>false, "message"=>"DB prepare failed (insert comment)", "error"=>$conn->error]);
$ins->bind_param('iis', $post_id, $user_id, $text);
if (!$ins->execute()) json_response(["success"=>false, "message"=>"Failed to insert comment", "error"=>$ins->error]);

$comment_id = $conn->insert_id;

// Return the inserted comment with user info
$q = $conn->prepare("SELECT c.id, c.post_id, c.user_id, c.text, c.timestamp, u.username, u.imageBase64 as userProfileImage FROM comments c JOIN users u ON c.user_id = u.id WHERE c.id = ? LIMIT 1");
$q->bind_param('i', $comment_id);
$q->execute();
$res = $q->get_result();
$row = $res->fetch_assoc();

json_response(["success"=>true, "comment" => $row]);

?>
