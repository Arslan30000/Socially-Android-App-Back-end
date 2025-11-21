<?php
require "db.php";
require "helpers.php";

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) $data = $_POST;

$token = get_bearer_token();
if (!$token) json_response(["success" => false, "message" => "No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows === 0) json_response(["success" => false, "message" => "Invalid token"]);
$user = $res->fetch_assoc();
$user_id = $user['user_id'];
$stmt->close();

$fcm_token = isset($data['fcm_token']) ? $data['fcm_token'] : '';

if (empty($fcm_token)) {
    json_response(["success" => false, "message" => "fcm_token is required"]);
}

// Use INSERT ... ON DUPLICATE KEY UPDATE to either insert a new token or update the existing one
$update_stmt = $conn->prepare(
    "INSERT INTO fcm_tokens (user_id, fcm_token) VALUES (?, ?) ON DUPLICATE KEY UPDATE fcm_token = ?"
);
$update_stmt->bind_param("iss", $user_id, $fcm_token, $fcm_token);

if ($update_stmt->execute()) {
    json_response(["success" => true, "message" => "FCM token updated"]);
} else {
    json_response(["success" => false, "message" => "Failed to update FCM token"]);
}
$update_stmt->close();
?>
