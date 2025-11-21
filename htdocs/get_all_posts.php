<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "No token provided"]);
}

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
if (!$stmt) json_response(["success"=>false,"message"=>"Invalid token"]);
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid or expired token"]);
$stmt->bind_result($user_id);
$stmt->fetch();

// Return all posts ordered by timestamp desc
$stmt2 = $conn->prepare("SELECT p.id, p.user_id, p.caption, p.postImage, p.timestamp, u.username, u.imageBase64 FROM posts p JOIN users u ON p.user_id = u.id ORDER BY p.timestamp DESC");
if (!$stmt2) json_response(["success"=>false,"message"=>"DB prepare failed","error"=>$conn->error]);
$stmt2->execute();
$res = $stmt2->get_result();

$posts = [];
while ($row = $res->fetch_assoc()) {
    $posts[] = [
        "postId" => (int)$row['id'],
        "userId" => (int)$row['user_id'],
        "username" => $row['username'],
        "caption" => $row['caption'],
        "postImage" => $row['postImage'],
        "userProfileImage" => $row['imageBase64'],
        "timestamp" => (int)$row['timestamp']
    ];
}

json_response(["success"=>true, "posts"=>$posts]);
?>
