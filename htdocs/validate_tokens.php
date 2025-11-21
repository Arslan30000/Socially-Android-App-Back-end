<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
$stmt->bind_param("s",$token);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    json_response(["success"=>false,"message"=>"Invalid token"]);
} else {
    json_response(["success"=>true,"message"=>"Token valid"]);
}
?>
