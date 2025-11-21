<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) {
    json_response(["success"=>false,"message"=>"No token provided"]);
}


$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s",$token);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    json_response(["success"=>false,"message"=>"Invalid or expired token"]);
}

$stmt->bind_result($user_id);
$stmt->fetch();

$stmt2 = $conn->prepare("SELECT id, username, name, lastname, dob, email, imageBase64, bio FROM users WHERE id=? LIMIT 1");
$stmt2->bind_param("i",$user_id);
$stmt2->execute();
$stmt2->store_result();
$stmt2->bind_result($id, $username, $name, $lastname, $dob, $email, $imageBase64, $bio);
$stmt2->fetch();

$user = [
    "id" => $id,
    "username" => $username,
    "name" => $name,
    "lastname" => $lastname,
    "dob" => $dob,
    "email" => $email,
    "imageBase64" => $imageBase64,
    "bio" => $bio
];

json_response(["success"=>true,"user"=>$user]);
?>
