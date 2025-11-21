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

// If no one is followed, return empty stories
if (empty($following_ids)) {
    json_response([
        "success" => true,
        "stories" => []
    ]);
}

// Get latest story from each followed user
$placeholders = implode(',', array_fill(0, count($following_ids), '?'));
$stmt3 = $conn->prepare("
    SELECT s.id, s.user_id, s.storyImage, s.timestamp, u.username, u.imageBase64
    FROM stories s
    JOIN users u ON s.user_id = u.id
    WHERE s.user_id IN ($placeholders)
    ORDER BY s.user_id, s.timestamp DESC
");

$stmt3->bind_param(str_repeat('i', count($following_ids)), ...$following_ids);
$stmt3->execute();
$result = $stmt3->get_result();

// Group stories by user and get latest one for each
$stories_by_user = [];
$latest_stories = [];

while ($row = $result->fetch_assoc()) {
    if (!isset($stories_by_user[$row['user_id']])) {
        $stories_by_user[$row['user_id']] = true;
        $latest_stories[] = [
            "id" => (int)$row['id'],
            "userId" => (int)$row['user_id'],
            "username" => $row['username'],
            "userProfilePicture" => $row['imageBase64'],
            "storyImage" => $row['storyImage'],
            "timestamp" => (int)$row['timestamp']
        ];
    }
}

// Get current user's profile for "Your Story" button
$stmt4 = $conn->prepare("SELECT id, username, imageBase64 FROM users WHERE id=? LIMIT 1");
$stmt4->bind_param("i", $user_id);
$stmt4->execute();
$result4 = $stmt4->get_result();
$user_profile = $result4->fetch_assoc();

$your_story = [
    "id" => -1,
    "userId" => (int)$user_profile['id'],
    "username" => "Your Story",
    "userProfilePicture" => $user_profile['imageBase64'],
    "storyImage" => "",
    "timestamp" => 0,
    "isAddButton" => true
];

// If a specific user_id param is provided, return all stories for that user only
$requested_user = isset($_GET['user_id']) ? intval($_GET['user_id']) : null;
if ($requested_user) {
    // Fetch all stories for the requested user ordered by timestamp asc (or desc as desired)
    $stmtU = $conn->prepare(
        "SELECT s.id, s.user_id, s.storyImage, s.timestamp, u.username, u.imageBase64
         FROM stories s
         JOIN users u ON s.user_id = u.id
         WHERE s.user_id = ?
         ORDER BY s.timestamp ASC"
    );
    $stmtU->bind_param("i", $requested_user);
    $stmtU->execute();
    $resU = $stmtU->get_result();

    $user_stories = [];
    while ($row = $resU->fetch_assoc()) {
        $user_stories[] = [
            "id" => (int)$row['id'],
            "userId" => (int)$row['user_id'],
            "username" => $row['username'],
            "userProfilePicture" => $row['imageBase64'],
            "storyImage" => $row['storyImage'],
            "timestamp" => (int)$row['timestamp']
        ];
    }

    json_response([
        "success" => true,
        "your_story" => $your_story,
        "stories" => $user_stories
    ]);
}

// Default behavior: return latest story per followed user (feed)
json_response([
    "success" => true,
    "your_story" => $your_story,
    "stories" => $latest_stories
]);
?>
