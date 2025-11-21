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

json_response([
    "success" => true,
    "following_ids" => $following_ids,
    "count" => count($following_ids)
]);
?>
