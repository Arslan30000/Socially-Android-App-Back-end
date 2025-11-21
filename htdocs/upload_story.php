<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

require "db.php";
require "helpers.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(["success" => false, "message" => "Use POST"]);
}

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

$storyImage = $_POST['storyImage'] ?? '';

if (!$storyImage) {
    json_response(["success" => false, "message" => "Missing storyImage"]);
}

$timestamp = intval($_POST['timestamp'] ?? time() * 1000);

$stmt2 = $conn->prepare("INSERT INTO stories (user_id, storyImage, timestamp) VALUES (?, ?, ?)");
$stmt2->bind_param("isi", $user_id, $storyImage, $timestamp);

if ($stmt2->execute()) {
    json_response([
        "success" => true,
        "message" => "Story uploaded successfully",
        "story_id" => $conn->insert_id
    ]);
} else {
    json_response(["success" => false, "message" => "Failed to upload story"]);
}
?>
