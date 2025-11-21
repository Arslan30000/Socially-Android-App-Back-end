<?php
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

require "db.php";
require "helpers.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(["success" => false, "message" => "Use POST"]);
}

$username = $_POST['username'] ?? '';
$password = $_POST['password'] ?? '';

if (!$username || !$password) {
    json_response(["success" => false, "message" => "Missing"]);
}

// Fetch user
$stmt = $conn->prepare("SELECT id, username, name, lastname, dob, email, imageBase64, bio, password_hash FROM users WHERE username=? OR email=? LIMIT 1");
$stmt->bind_param("ss", $username, $username);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    json_response(["success" => false, "message" => "User not found"]);
}

$stmt->bind_result($id, $uname, $name, $lastname, $dob, $email, $imageBase64, $bio, $hash);
$stmt->fetch();

if (!password_verify($password, $hash)) {
    json_response(["success" => false, "message" => "Invalid credentials"]);
}

// Generate token
$token = generate_token(64);
$expiry = date('Y-m-d H:i:s', strtotime('+30 days'));

$stmt2 = $conn->prepare("INSERT INTO tokens (user_id, token, expires_at) VALUES (?, ?, ?)");
$stmt2->bind_param("iss", $id, $token, $expiry);
$stmt2->execute();

// Prepare response
$userData = [
    "id" => $id,
    "username" => $uname,
    "name" => $name,
    "lastname" => $lastname,
    "dob" => $dob,
    "email" => $email,
    "imageBase64" => $imageBase64,
    "bio" => $bio,
    "user_id" => $id
];

json_response([
    "success" => true,
    "token" => $token,
    "user_id" => $id,
    "user" => $userData
]);
?>
