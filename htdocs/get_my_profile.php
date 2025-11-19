<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "No token provided"]);
}

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
if (!$stmt) {
    json_response(["success" => false, "message" => "DB prepare failed (tokens)", "error" => $conn->error]);
}
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    json_response(["success" => false, "message" => "Invalid or expired token"]);
}

$stmt->bind_result($user_id);
$stmt->fetch();
$stmt->close();

// Fetch user details
$user_stmt = $conn->prepare("SELECT id, username, name, lastname, bio, imageBase64 FROM users WHERE id = ?");
$user_stmt->bind_param("i", $user_id);
$user_stmt->execute();
$user_result = $user_stmt->get_result();
$user = $user_result->fetch_assoc();
$user_stmt->close();

// Fetch user's posts
$posts_stmt = $conn->prepare("SELECT id, postImage, caption FROM posts WHERE user_id = ? ORDER BY created_at DESC");
$posts_stmt->bind_param("i", $user_id);
$posts_stmt->execute();
$posts_result = $posts_stmt->get_result();
$posts = [];
while ($row = $posts_result->fetch_assoc()) {
    $posts[] = $row;
}
$posts_stmt->close();

// Fetch user's stories
$stories_stmt = $conn->prepare("SELECT id, storyImage, timestamp FROM stories WHERE user_id = ? AND created_at > NOW() - INTERVAL 1 DAY ORDER BY created_at DESC");
$stories_stmt->bind_param("i", $user_id);
$stories_stmt->execute();
$stories_result = $stories_stmt->get_result();
$stories = [];
while ($row = $stories_result->fetch_assoc()) {
    $stories[] = $row;
}
$stories_stmt->close();

// Fetch follower and following counts
$followers_count_stmt = $conn->prepare("SELECT COUNT(*) FROM followers WHERE following_id = ?");
$followers_count_stmt->bind_param("i", $user_id);
$followers_count_stmt->execute();
$followers_count_stmt->bind_result($followers_count);
$followers_count_stmt->fetch();
$followers_count_stmt->close();

$following_count_stmt = $conn->prepare("SELECT COUNT(*) FROM followers WHERE follower_id = ?");
$following_count_stmt->bind_param("i", $user_id);
$following_count_stmt->execute();
$following_count_stmt->bind_result($following_count);
$following_count_stmt->fetch();
$following_count_stmt->close();

json_response([
    "success" => true,
    "user" => $user,
    "posts" => $posts,
    "stories" => $stories,
    "followers_count" => $followers_count,
    "following_count" => $following_count
]);
?>
