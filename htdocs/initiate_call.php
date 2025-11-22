<?php
require "db.php";
require "helpers.php";

// Get JSON body
$raw = file_get_contents('php://input');
$data = json_decode($raw, true);
if (!$data) $data = $_POST; // Fallback for form-data

// Authenticate user
$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "No token provided"]);
}

$stmt = $conn->prepare(
    "SELECT u.id as user_id, u.name, u.lastname 
     FROM users u 
     JOIN tokens t ON u.id = t.user_id 
     WHERE t.token = ? LIMIT 1"
);
$stmt->bind_param("s", $token);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows === 0) {
    json_response(["success" => false, "message" => "Invalid token"]);
}
$caller = $res->fetch_assoc();
$caller_id = $caller['user_id'];
$caller_name = trim($caller['name'] . ' ' . $caller['lastname']);
$stmt->close();

// Get parameters
$receiver_id = isset($data['receiver_id']) ? (int)$data['receiver_id'] : 0;
$channel_name = isset($data['channel_name']) ? $data['channel_name'] : '';
$call_type = isset($data['call_type']) ? $data['call_type'] : 'video'; // 'video' or 'audio'

if ($receiver_id <= 0 || empty($channel_name) || !in_array($call_type, ['video', 'audio'])) {
    json_response(["success" => false, "message" => "A valid receiver_id, channel_name, and call_type are required."]);
}

// Prevent duplicate pending calls for the same receiver
$check_stmt = $conn->prepare("SELECT id FROM pending_calls WHERE receiver_id = ?");
$check_stmt->bind_param("i", $receiver_id);
$check_stmt->execute();
if ($check_stmt->get_result()->num_rows > 0) {
    json_response(["success" => false, "message" => "That user already has a pending call."]);
}
$check_stmt->close();

// Insert the call into the pending_calls table
$insert_stmt = $conn->prepare(
    "INSERT INTO pending_calls (caller_id, caller_name, receiver_id, channel_name, call_type) VALUES (?, ?, ?, ?, ?)"
);
$insert_stmt->bind_param("isiss", $caller_id, $caller_name, $receiver_id, $channel_name, $call_type);

if ($insert_stmt->execute()) {
    json_response(["success" => true, "message" => "Call initiated and is now pending."]);
} else {
    json_response(["success" => false, "message" => "Failed to initiate call."]);
}
$insert_stmt->close();
?>