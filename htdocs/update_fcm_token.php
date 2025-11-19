<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success" => false, "message" => "No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success" => false, "message" => "Invalid token"]);
$stmt->bind_result($user_id);
$stmt->fetch();
$stmt->close();

$fcm_token = $_POST['fcm_token'] ?? '';

if (empty($fcm_token)) {
    json_response(["success" => false, "message" => "FCM token is required."]);
}

$stmt = $conn->prepare("UPDATE users SET fcm_token = ? WHERE id = ?");
$stmt->bind_param("si", $fcm_token, $user_id);

if ($stmt->execute()) {
    json_response(["success" => true, "message" => "FCM token updated successfully."]);
} else {
    json_response(["success" => false, "message" => "Failed to update FCM token."]);
}

$stmt->close();
?>
