<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);
require "db.php";
require "helpers.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') json_response(["success"=>false,"message"=>"Use POST"]);

$username = $_POST['username'] ?? '';
$name = $_POST['name'] ?? '';
$lastname = $_POST['lastname'] ?? '';
$dob = $_POST['dob'] ?? null;
$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';
$imageBase64 = $_POST['imageBase64'] ?? '';

if (!$username || !$email || !$password) json_response(["success"=>false,"message"=>"Missing required fields"]);

$stmt = $conn->prepare("SELECT id FROM users WHERE username=? OR email=?");
$stmt->bind_param("ss",$username,$email);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows > 0) json_response(["success"=>false,"message"=>"Username or email exists"]);
$stmt->close();

$hash = password_hash($password, PASSWORD_DEFAULT);

$stmt = $conn->prepare("INSERT INTO users (username, name, lastname, dob, email, password_hash, imageBase64) VALUES (?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("sssssss", $username, $name, $lastname, $dob, $email, $hash, $imageBase64);
if ($stmt->execute()) {
    $uid = $conn->insert_id;
    $token = generate_token(64);
    $expiry = date('Y-m-d H:i:s', strtotime('+30 days'));
    $stmt2 = $conn->prepare("INSERT INTO tokens (user_id, token, expires_at) VALUES (?, ?, ?)");
    $stmt2->bind_param("iss",$uid,$token,$expiry);
    $stmt2->execute();
    json_response(["status"=>"success","message"=>"User created","token"=>$token,"user_id"=>$uid]);
} else {
    json_response(["status"=>"error","message"=>"Insert failed"]);
}
?>
