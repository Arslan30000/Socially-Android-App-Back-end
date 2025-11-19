<?php
require "db.php";
require "helpers.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(["success" => false, "message" => "Use POST"]);
}

$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "No token provided"]);
}

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
if (!$stmt) {
    json_response(["success" => false, "message" => "DB prepare failed (tokens)", "error" => $conn->error]);
}
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();

if ($stmt->num_rows === 0) {
    json_response(["success" => false, "message" => "Invalid or expired token"]);
}

$stmt->bind_result($user_id);
$stmt->fetch();
$stmt->close();

if (!isset($_FILES['file'])) {
    json_response(["success" => false, "message" => "No file uploaded"]);
}

$file = $_FILES['file'];

if ($file['error'] !== UPLOAD_ERR_OK) {
    json_response(["success" => false, "message" => "File upload error: " . $file['error']]);
}

$uploadDir = 'uploads/';
if (!is_dir($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

$filename = uniqid() . '-' . basename($file['name']);
$uploadPath = $uploadDir . $filename;

if (move_uploaded_file($file['tmp_name'], $uploadPath)) {
    $url = 'http://' . $_SERVER['HTTP_HOST'] . dirname($_SERVER['PHP_SELF']) . '/' . $uploadPath;
    json_response(["success" => true, "url" => $url]);
} else {
    json_response(["success" => false, "message" => "Failed to move uploaded file"]);
}
?>
