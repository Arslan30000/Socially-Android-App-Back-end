<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
if (!$stmt) json_response(["success"=>false,"message"=>"Invalid token"]);
$stmt->bind_param("s", $token); $stmt->execute(); $stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid or expired token"]);
$stmt->bind_result($user_id);
$stmt->fetch();
$stmt->close();

$sql = "
SELECT
    c.id AS conversationId,
    other_user.id AS userId,
    other_user.username,
    other_user.imageBase64 AS profileImage,
    last_msg.content AS lastMessage,
    last_msg.type AS lastMessageType,
    last_msg.sender_id AS lastSenderId,
    last_msg.created_at AS lastMessageAt,
    unread.unreadCount,
    last_msg.id as lastMessageId,
    last_msg.is_seen as lastMessageSeen
FROM
    conversations c
JOIN
    -- Determine the 'other' user in the conversation
    users other_user ON other_user.id = IF(c.user_a = ?, c.user_b, c.user_a)
LEFT JOIN
    -- Get the last message for each conversation
    messages last_msg ON c.last_message_id = last_msg.id
LEFT JOIN
    -- Get the count of unread messages for the current user
    (SELECT conversation_id, COUNT(*) AS unreadCount
     FROM messages
     WHERE receiver_id = ? AND is_seen = 0 AND is_deleted = 0
     GROUP BY conversation_id) AS unread ON unread.conversation_id = c.id
WHERE
    c.user_a = ? OR c.user_b = ?
ORDER BY
    c.last_updated DESC;
";

$stmt2 = $conn->prepare($sql);
if (!$stmt2) {
    json_response(["success" => false, "message" => "DB prepare failed", "error" => $conn->error]);
}
$stmt2->bind_param("iiii", $user_id, $user_id, $user_id, $user_id);
$stmt2->execute();
$res = $stmt2->get_result();

$out = [];
while ($row = $res->fetch_assoc()) {
    $out[] = [
        "conversationId" => (int)$row['conversationId'],
        "userId" => (int)$row['userId'],
        "username" => $row['username'],
        "profileImage" => $row['profileImage'],
        "lastMessage" => $row['lastMessage'],
        "lastMessageType" => $row['lastMessageType'],
        "lastSenderId" => $row['lastSenderId'] ? (int)$row['lastSenderId'] : null,
        "lastMessageAt" => $row['lastMessageAt'],
        "unreadCount" => $row['unreadCount'] ? (int)$row['unreadCount'] : 0,
        "lastMessageId" => $row['lastMessageId'] ? (int)$row['lastMessageId'] : null,
        "lastMessageSeen" => $row['lastMessageSeen'] ? (int)$row['lastMessageSeen'] : 0
    ];
}
$stmt2->close();

json_response(["success"=>true, "conversations"=>$out]);
?>
