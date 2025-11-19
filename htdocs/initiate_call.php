<?php
require "db.php";
require "helpers.php";


$FCM_SERVER_KEY = "YOUR_FCM_SERVER_KEY";

$token = get_bearer_token();
if (!$token) json_response(["success" => false, "message" => "No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success" => false, "message" => "Invalid token"]);
$stmt->bind_result($caller_id);
$stmt->fetch();
$stmt->close();

$recipient_id = $_POST['recipient_id'] ?? 0;
$channel_name = $_POST['channel_name'] ?? '';
$call_type = $_POST['call_type'] ?? 'video'; // 'video' or 'audio'

if (empty($recipient_id) || empty($channel_name)) {
    json_response(["success" => false, "message" => "Recipient ID and channel name are required."]);
}

// Get the recipient's FCM token from your database
// (Assuming you have a 'fcm_token' column in your 'users' table)
$stmt = $conn->prepare("SELECT fcm_token FROM users WHERE id = ? LIMIT 1");
$stmt->bind_param("i", $recipient_id);
$stmt->execute();
$stmt->bind_result($recipient_fcm_token);
$stmt->fetch();
$stmt->close();

if (empty($recipient_fcm_token)) {
    json_response(["success" => false, "message" => "Recipient does not have a registered device."]);
}

// Prepare the notification payload
$notification_data = [
    "to" => $recipient_fcm_token,
    "data" => [
        "type" => "incoming_call",
        "caller_id" => $caller_id,
        "channel_name" => $channel_name,
        "call_type" => $call_type
    ],
    "priority" => "high"
];

// Send the notification using cURL
$ch = curl_init("https://fcm.googleapis.com/fcm/send");
curl_setopt($ch, CURLOPT_HTTPHEADER, [
    "Authorization: key=" . $FCM_SERVER_KEY,
    "Content-Type: application/json"
]);
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($notification_data));
curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);

$response = curl_exec($ch);
$http_code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
curl_close($ch);

if ($http_code == 200) {
    json_response(["success" => true, "message" => "Call initiated successfully."]);
} else {
    json_response(["success" => false, "message" => "Failed to send call notification.", "fcm_response" => $response]);
}
?>
