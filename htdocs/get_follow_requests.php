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
$stmt->bind_result($to_user_id);
$stmt->fetch();

$stmt2 = $conn->prepare("SELECT fr.from_user_id, u.username, u.imageBase64, fr.created_at FROM follow_requests fr JOIN users u ON fr.from_user_id = u.id WHERE fr.to_user_id = ? ORDER BY fr.created_at DESC");
$stmt2->bind_param("i", $to_user_id);
$stmt2->execute();
$res = $stmt2->get_result();

$requests = [];
while ($row = $res->fetch_assoc()) {
    $requests[] = [
        "from_user_id" => (int)$row['from_user_id'],
        "username" => $row['username'],
        "imageBase64" => $row['imageBase64'],
        "created_at" => $row['created_at']
    ];
}

json_response(["success"=>true, "requests"=>$requests]);
?>