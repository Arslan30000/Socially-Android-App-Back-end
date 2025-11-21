<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "No token provided"]);
}

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    json_response(["success" => false, "message" => "Invalid or expired token"]);
}

$stmt->bind_result($user_id);
$stmt->fetch();

// Get all users this user is following
$stmt2 = $conn->prepare("SELECT following_id FROM followers WHERE follower_id=?");
$stmt2->bind_param("i", $user_id);
$stmt2->execute();
$result = $stmt2->get_result();

$following_ids = [];
while ($row = $result->fetch_assoc()) {
    $following_ids[] = $row['following_id'];
}

// Always include current user's own id so their posts appear in feed
$following_ids[] = $user_id;

// Get posts from all followed users, ordered by timestamp (newest first)
$placeholders = implode(',', array_fill(0, count($following_ids), '?'));
$sql = "SELECT p.id, p.user_id, p.caption, p.postImage, p.timestamp, u.username, u.imageBase64
    FROM posts p
    JOIN users u ON p.user_id = u.id
    WHERE p.user_id IN ($placeholders)
    ORDER BY p.timestamp DESC";

$stmt3 = $conn->prepare($sql);
if (!$stmt3) {
    json_response(["success"=>false, "message"=>"DB prepare failed (feed)", "error"=>$conn->error]);
}

// Bind parameters by reference (call_user_func_array) to support dynamic IN list
$types = str_repeat('i', count($following_ids));
$bind_params = array_merge([$types], $following_ids);
$refs = [];
foreach ($bind_params as $key => $value) {
    $refs[$key] = &$bind_params[$key];
}
call_user_func_array([$stmt3, 'bind_param'], $refs);
$stmt3->execute();
$result = $stmt3->get_result();

$posts = [];
while ($row = $result->fetch_assoc()) {
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

// Enrich each post with likesCount, commentsCount and whether current user liked it
$likesStmt = $conn->prepare("SELECT COUNT(*) as cnt FROM post_likes WHERE post_id=?");
$commentsStmt = $conn->prepare("SELECT COUNT(*) as cnt FROM comments WHERE post_id=?");
$isLikedStmt = $conn->prepare("SELECT 1 FROM post_likes WHERE post_id=? AND user_id=? LIMIT 1");

if (!$likesStmt || !$commentsStmt || !$isLikedStmt) {
    json_response(["success"=>false, "message"=>"DB prepare failed (counts)", "error"=>$conn->error]);
}

for ($i = 0; $i < count($posts); $i++) {
    $postId = $posts[$i]['postId'];

    // likes count
    $likesStmt->bind_param('i', $postId);
    $likesStmt->execute();
    $likesRes = $likesStmt->get_result();
    $likesRow = $likesRes->fetch_assoc();
    $likesCount = (int)$likesRow['cnt'];

    // comments count
    $commentsStmt->bind_param('i', $postId);
    $commentsStmt->execute();
    $commentsRes = $commentsStmt->get_result();
    $commentsRow = $commentsRes->fetch_assoc();
    $commentsCount = (int)$commentsRow['cnt'];

    // is liked by current user
    $isLikedStmt->bind_param('ii', $postId, $user_id);
    $isLikedStmt->execute();
    $isLikedRes = $isLikedStmt->get_result();
    $isLiked = ($isLikedRes->num_rows > 0);

    $posts[$i]['likesCount'] = $likesCount;
    $posts[$i]['commentsCount'] = $commentsCount;
    $posts[$i]['isLiked'] = $isLiked;
}

json_response([
    "success" => true,
    "posts" => $posts
]);
?>
