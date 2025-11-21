<?php
require "db.php";
require "helpers.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_response(["success"=>false,"message"=>"Use POST"]);

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid token"]);
$stmt->bind_result($current_user_id);
$stmt->fetch();

$from_user_id = intval($_POST['from_user_id'] ?? 0);
if ($from_user_id <= 0) json_response(["success"=>false,"message"=>"Missing from_user_id"]);

// Insert follower relation: follower_id = from_user_id, following_id = current_user
$stmtf = $conn->prepare("INSERT INTO followers (follower_id, following_id, created_at) VALUES (?, ?, NOW())");
$stmtf->bind_param("ii", $from_user_id, $current_user_id);

if (!$stmtf->execute()) {
    json_response(["success"=>false,"message"=>"Failed to add follower"]);
}

// Optionally insert reciprocal? No, followers table design covers follower->following mapping.
// Remove the follow_request row
$stmt2 = $conn->prepare("DELETE FROM follow_requests WHERE from_user_id=? AND to_user_id=?");
$stmt2->bind_param("ii", $from_user_id, $current_user_id);
$stmt2->execute();

json_response(["success"=>true, "message"=>"Follow request accepted"]);
?>