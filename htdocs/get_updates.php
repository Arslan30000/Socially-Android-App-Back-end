<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "No token provided"]);
}

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows === 0) {
    json_response(["success" => false, "message" => "Invalid token"]);
}
$user = $res->fetch_assoc();
$user_id = $user['user_id'];
$stmt->close();

$response_data = [
    "success" => true,
    "updates" => [
        "new_requests_count" => 0,
        "new_messages" => []
    ]
];

// 1. Check for new follow requests
$req_stmt = $conn->prepare("SELECT COUNT(*) as count FROM follow_requests WHERE to_user_id = ?");
$req_stmt->bind_param("i", $user_id);
$req_stmt->execute();
$req_res = $req_stmt->get_result();
$req_row = $req_res->fetch_assoc();
$response_data["updates"]["new_requests_count"] = (int)$req_row['count'];
$req_stmt->close();


// 2. Check for new messages in recent conversations
$conv_stmt = $conn->prepare(
    "SELECT c.id as conversation_id, c.user_a, c.user_b, m.*,
           u.name, u.lastname, u.username
    FROM conversations c
    JOIN messages m ON c.last_message_id = m.id
    JOIN users u ON u.id = IF(c.user_a = ?, c.user_b, c.user_a)
    WHERE (c.user_a = ? OR c.user_b = ?)
    ORDER BY m.created_at DESC
    LIMIT 20"
);
$conv_stmt->bind_param("iii", $user_id, $user_id, $user_id);
$conv_stmt->execute();
$conv_res = $conv_stmt->get_result();

$new_messages = [];
while ($row = $conv_res->fetch_assoc()) {
    $new_messages[] = [
        "conversation_id" => $row['conversation_id'],
        "sender_id" => $row['sender_id'],
        "content" => $row['content'],
        "created_at" => strtotime($row['created_at']),
        "sender_name" => trim($row['name'] . ' ' . $row['lastname'])
    ];
}
$response_data["updates"]["new_messages"] = $new_messages;
$conv_stmt->close();

json_response($response_data);
?>