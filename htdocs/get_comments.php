<?php
require "db.php";
require "helpers.php";

$post_id = isset($_GET['post_id']) ? intval($_GET['post_id']) : 0;
if ($post_id <= 0) json_response(["success"=>false, "message"=>"post_id required"]);

$q = $conn->prepare("SELECT c.id, c.post_id, c.user_id, c.text, c.timestamp, u.username, u.imageBase64 as userProfileImage FROM comments c JOIN users u ON c.user_id = u.id WHERE c.post_id = ? ORDER BY c.timestamp ASC");
if (!$q) json_response(["success"=>false, "message"=>"DB prepare failed (get comments)", "error"=>$conn->error]);
$q->bind_param('i', $post_id);
$q->execute();
$res = $q->get_result();

$comments = [];
while ($row = $res->fetch_assoc()) {
    $comments[] = [
        "id" => (int)$row['id'],
        "postId" => (int)$row['post_id'],
        "userId" => (int)$row['user_id'],
        "username" => $row['username'],
        "userProfileImage" => $row['userProfileImage'],
        "text" => $row['text'],
        "timestamp" => $row['timestamp']
    ];
}

json_response(["success"=>true, "comments" => $comments]);

?>
