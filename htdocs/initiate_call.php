<?php
require "db.php";
require "helpers.php";
require "fcm_helper.php";

$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) $data = $_POST;

$token = get_bearer_token();
if (!$token) json_response(["success" => false, "message" => "No token provided"]);

$stmt = $conn->prepare("SELECT user_id, (SELECT username FROM users WHERE id = user_id) as username FROM tokens WHERE token=? LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows === 0) json_response(["success" => false, "message" => "Invalid token"]);
$caller = $res->fetch_assoc();
$caller_id = $caller['user_id'];
$caller_name = $caller['username'];
$stmt->close();

$receiver_id = isset($data['receiver_id']) ? (int)$data['receiver_id'] : 0;
$channel_name = isset($data['channel_name']) ? $data['channel_name'] : '';
$call_type = isset($data['call_type']) ? $data['call_type'] : 'video';

if ($receiver_id <= 0 || empty($channel_name)) {
    json_response(["success" => false, "message" => "receiver_id and channel_name are required"]);
}

// Get the receiver's FCM token from the new fcm_tokens table
$fcm_stmt = $conn->prepare("SELECT fcm_token FROM fcm_tokens WHERE user_id = ?");
$fcm_stmt->bind_param("i", $receiver_id);
$fcm_stmt->execute();
$fcm_stmt->bind_result($fcm_token);
$fcm_stmt->fetch();
$fcm_stmt->close();

if (empty($fcm_token)) {
    json_response(["success" => false, "message" => "Receiver is not available for calls"]);
}

// Prepare the data payload for the notification
$notification_data = [
    "type" => "incoming_call",
    "caller_name" => $caller_name,
    "channel_name" => $channel_name,
    "call_type" => $call_type
];

// Send the notification using our new helper
$result = send_fcm_notification($fcm_token, $notification_data);

if (isset($result['name'])) {
    json_response(["success" => true, "message" => "Call initiated successfully"]);
} else {
    error_log("FCM Send Error: " . json_encode($result));
    json_response(["success" => false, "message" => "Failed to notify receiver", "fcm_response" => $result]);
}
?>
