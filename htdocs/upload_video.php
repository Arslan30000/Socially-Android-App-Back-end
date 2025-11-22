<?php
require "db.php";
require "helpers.php";

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    json_response(["success" => false, "message" => "Only POST is allowed"]);
}

$token = get_bearer_token();
if (!$token) {
    json_response(["success" => false, "message" => "Authentication required"]);
}

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token = ?");
$stmt->bind_param("s", $token);
$stmt->execute();
$result = $stmt->get_result();
if ($result->num_rows === 0) {
    json_response(["success" => false, "message" => "Invalid token"]);
}
$stmt->close();

if (isset($_FILES['video'])) {
    $target_dir = "uploads/";
    if (!is_dir($target_dir)) {
        mkdir($target_dir, 0777, true);
    }
    $target_file = $target_dir . basename($_FILES["video"]["name"]);
    $videoFileType = strtolower(pathinfo($target_file, PATHINFO_EXTENSION));

    // Allow certain file formats
    if ($videoFileType != "mp4" && $videoFileType != "avi" && $videoFileType != "mov") {
        json_response(["success" => false, "message" => "Sorry, only MP4, AVI & MOV files are allowed."]);
    }

    if (move_uploaded_file($_FILES["video"]["tmp_name"], $target_file)) {
        $url = get_server_url() . '/' . $target_file;
        json_response(["success" => true, "url" => $url]);
    } else {
        json_response(["success" => false, "message" => "Sorry, there was an error uploading your file."]);
    }
} else {
    json_response(["success" => false, "message" => "No video file provided"]);
}

function get_server_url() {
    $protocol = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off' || $_SERVER['SERVER_PORT'] == 443) ? "https://" : "http://";
    $host = $_SERVER['HTTP_HOST'];
    $script = $_SERVER['SCRIPT_NAME'];
    $path = substr($script, 0, strrpos($script, '/'));
    return $protocol . $host . $path;
}
?>