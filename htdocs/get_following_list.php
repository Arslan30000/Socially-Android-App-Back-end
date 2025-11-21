<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid token"]);
$stmt->bind_result($current_user_id);
$stmt->fetch();

$user_id = intval($_GET['user_id'] ?? $current_user_id);

$stmt2 = $conn->prepare("SELECT f.following_id, u.username, u.imageBase64 FROM followers f JOIN users u ON f.following_id = u.id WHERE f.follower_id = ? ORDER BY f.created_at DESC");
$stmt2->bind_param("i", $user_id);
$stmt2->execute();
$res = $stmt2->get_result();

$list = [];
while ($row = $res->fetch_assoc()) {
    $list[] = [
        "id" => (int)$row['following_id'],
        "username" => $row['username'],
        "imageBase64" => $row['imageBase64']
    ];
}

json_response(["success"=>true, "list"=>$list]);
?>