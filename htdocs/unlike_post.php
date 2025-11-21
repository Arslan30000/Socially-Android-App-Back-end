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

// Delete like if exists
$del = $conn->prepare("DELETE FROM post_likes WHERE post_id=? AND user_id=?");
if (!$del) json_response(["success"=>false, "message"=>"DB prepare failed (delete like)", "error"=>$conn->error]);
$del->bind_param('ii', $post_id, $user_id);
if (!$del->execute()) {
    json_response(["success"=>false, "message"=>"Failed to remove like", "error"=>$del->error]);
}

$c = $conn->prepare("SELECT COUNT(*) as cnt FROM post_likes WHERE post_id=?");
$c->bind_param('i', $post_id);
$c->execute();
$res = $c->get_result();
$r = $res->fetch_assoc();

json_response(["success"=>true, "likesCount" => (int)$r['cnt']]);

?>
