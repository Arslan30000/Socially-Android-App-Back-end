<?php
$response_sent = false;

// Shutdown handler to catch fatal errors and return JSON instead of a raw 500 page
register_shutdown_function(function() use (&$response_sent) {
    $err = error_get_last();
    if ($err !== null && !$response_sent) {
        error_log("upload_post.php fatal: " . print_r($err, true));
        http_response_code(500);
        header('Content-Type: application/json; charset=utf-8');
        echo json_encode(["success"=>false, "message"=>"Server error during upload", "error"=>$err]);
        // no exit here; shutdown
    }
});

// Protect against extremely large POST bodies (base64 images). Return a clear error instead of causing a 500.
$MAX_POST_BYTES = 5 * 1024 * 1024; // 5 MB - tune as needed
$contentLen = isset($_SERVER['CONTENT_LENGTH']) ? (int)$_SERVER['CONTENT_LENGTH'] : 0;
if ($contentLen > $MAX_POST_BYTES) {
    http_response_code(413);
    echo json_encode(["success" => false, "message" => "Payload too large (limit 5MB). Please compress image or upload smaller file."]);
    exit;
}

ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

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

// Quick check: ensure `posts` table exists to avoid runtime DB errors
$chk = $conn->query("SHOW TABLES LIKE 'posts'");
if (!$chk || $chk->num_rows === 0) {
    error_log("upload_post.php error: posts table missing");
    json_response(["success"=>false, "message"=>"Server misconfiguration: posts table missing"]); 
}

// Accept either a base64 `postImage` payload OR a `postImageUrl` (path returned by upload_media.php)
$postImage = $_POST['postImage'] ?? '';
$postImageUrl = $_POST['postImageUrl'] ?? '';
$caption = $_POST['caption'] ?? '';
$timestamp = intval($_POST['timestamp'] ?? time() * 1000);

if (empty($postImage) && empty($postImageUrl)) {
    json_response(["success" => false, "message" => "Missing postImage or postImageUrl"]);
}

// If a postImageUrl is provided, prefer it (client likely uploaded file via `upload_media.php`)
if (!empty($postImageUrl)) {
    $storedImage = $postImageUrl;
} else {
    $storedImage = $postImage; // legacy: storing base64 string in DB (may be large)
}

// Prepare insert
// Insert post; `postImage` column stores either a URL/path or base64 string depending on client
$stmt2 = $conn->prepare("INSERT INTO posts (user_id, caption, postImage, timestamp) VALUES (?, ?, ?, ?)");
if (!$stmt2) {
    json_response(["success" => false, "message" => "DB prepare failed (insert)", "error" => $conn->error]);
}

$stmt2->bind_param("issi", $user_id, $caption, $storedImage, $timestamp);

if ($stmt2->execute()) {
    json_response([
        "success" => true,
        "message" => "Post uploaded successfully",
        "post_id" => $conn->insert_id
    ]);
} else {
    json_response(["success" => false, "message" => "Failed to upload post", "error" => $stmt2->error]);
}
?>
