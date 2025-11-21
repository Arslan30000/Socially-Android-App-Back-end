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
if ($post_id <= 0) json_response(["success"=>false, "message"=>"post_id required"]);

// Ensure post exists
$p = $conn->prepare("SELECT id FROM posts WHERE id=? LIMIT 1");
$p->bind_param('i', $post_id);
$p->execute();
$p->store_result();
if ($p->num_rows === 0) json_response(["success"=>false, "message"=>"Post not found"]);

// Check already liked
$check = $conn->prepare("SELECT 1 FROM post_likes WHERE post_id=? AND user_id=? LIMIT 1");
$check->bind_param('ii', $post_id, $user_id);
$check->execute();
$check->store_result();
if ($check->num_rows > 0) {
    // already liked, return current count
    $c = $conn->prepare("SELECT COUNT(*) as cnt FROM post_likes WHERE post_id=?");
    $c->bind_param('i', $post_id);
    $c->execute();
    $res = $c->get_result();
    $r = $res->fetch_assoc();
    json_response(["success"=>true, "likesCount" => (int)$r['cnt'], "message"=>"Already liked"]);
}

$ins = $conn->prepare("INSERT INTO post_likes (post_id, user_id, created_at) VALUES (?, ?, NOW())");
if (!$ins) json_response(["success"=>false, "message"=>"DB prepare failed (insert like)", "error"=>$conn->error]);
$ins->bind_param('ii', $post_id, $user_id);
if (!$ins->execute()) {
    json_response(["success"=>false, "message"=>"Failed to insert like", "error"=>$ins->error]);
}

$c = $conn->prepare("SELECT COUNT(*) as cnt FROM post_likes WHERE post_id=?");
$c->bind_param('i', $post_id);
$c->execute();
$res = $c->get_result();
$r = $res->fetch_assoc();

json_response(["success"=>true, "likesCount" => (int)$r['cnt']]);

?>
