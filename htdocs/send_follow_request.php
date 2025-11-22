<?php
require "db.php";
require "helpers.php";
require_once "send_notification.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_response(["success"=>false,"message"=>"Use POST"]);

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id, (SELECT username FROM users WHERE id = user_id) as username FROM tokens WHERE token=? LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid token"]);
$caller = $res->fetch_assoc();
$from_user_id = $caller['user_id'];
$from_user_name = $caller['username'];
$stmt->close();

$to_user_id = intval($_POST['to_user_id'] ?? 0);
if ($to_user_id <= 0) json_response(["success"=>false,"message"=>"Missing to_user_id"]);

// ... (rest of your existing validation logic)

// Insert request
$stmt2 = $conn->prepare("INSERT INTO follow_requests (from_user_id, to_user_id, created_at) VALUES (?, ?, NOW())");
$stmt2->bind_param("ii", $from_user_id, $to_user_id);
if ($stmt2->execute()) {
    // Send notification
    $notification_title = "New Follow Request";
    $notification_body = $from_user_name . " wants to follow you.";
    send_notification($to_user_id, $notification_title, $notification_body);

    json_response(["success"=>true,"message"=>"Request sent"]);
} else {
    json_response(["success"=>false,"message"=>"Failed to send request"]);
}
?>
