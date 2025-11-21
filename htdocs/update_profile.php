<?php
require_once 'db.php';
require_once 'validate_tokens.php';

header("Content-Type: application/json");

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
    exit;
}

$user_id = validate_token();
if (!$user_id) {
    echo json_encode(["success" => false, "message" => "Unauthorized"]);
    exit;
}

$name       = $_POST['name']       ?? '';
$username   = $_POST['username']   ?? '';
$bio        = $_POST['bio']        ?? '';
$email      = $_POST['email']      ?? '';
$phone      = $_POST['phone']      ?? '';
$gender     = $_POST['gender']     ?? '';
$imageBase64 = $_POST['imageBase64'] ?? '';

if (empty($name) || empty($username) || empty($email)) {
    echo json_encode(["success" => false, "message" => "Required fields missing"]);
    exit;
}

$check = $conn->prepare("SELECT id FROM users WHERE username = ? AND id != ?");
$check->bind_param("si", $username, $user_id);
$check->execute();
$res = $check->get_result();
if ($res->num_rows > 0) {
    echo json_encode(["success" => false, "message" => "Username already taken"]);
    exit;
}
$check->close();

$sql = "UPDATE users SET name=?, username=?, bio=?, email=?, phone=?, gender=?";
$params = [$name, $username, $bio, $email, $phone, $gender];
$types = "ssssss";

if (!empty($imageBase64)) {
    $sql .= ", imageBase64=?";
    $params[] = $imageBase64;
    $types .= "s";
}

$sql .= " WHERE id=?";
$params[] = $user_id;
$types .= "i";

$stmt = $conn->prepare($sql);

if (!$stmt) {
    echo json_encode([
        "success" => false,
        "message" => "Prepare failed",
        "error" => $conn->error
    ]);
    exit;
}

$stmt->bind_param($types, ...$params);

if (!$stmt->execute()) {
    echo json_encode([
        "success" => false,
        "message" => "Update failed",
        "error" => $stmt->error
    ]);
    exit;
}
$stmt->close();

$u = $conn->prepare("SELECT id, username, name, bio, email, phone, gender, imageBase64 FROM users WHERE id=? LIMIT 1");
$u->bind_param("i", $user_id);
$u->execute();
$user = $u->get_result()->fetch_assoc();
$u->close();

$followers = $conn->query("SELECT COUNT(*) AS c FROM followers WHERE following_id = $user_id")->fetch_assoc()['c'];
$following = $conn->query("SELECT COUNT(*) AS c FROM followers WHERE follower_id = $user_id")->fetch_assoc()['c'];
$posts     = $conn->query("SELECT COUNT(*) AS c FROM posts WHERE user_id = $user_id")->fetch_assoc()['c'];

echo json_encode([
    "success" => true,
    "message" => "Profile updated successfully",
    "user" => [
        "id"        => (int)$user['id'],
        "username"  => $user['username'],
        "name"      => $user['name'],
        "bio"       => $user['bio'],
        "email"     => $user['email'],
        "phone"     => $user['phone'],
        "gender"    => $user['gender'],
        "imageBase64" => $user['imageBase64']
    ],
    "counts" => [
        "followers" => (int)$followers,
        "following" => (int)$following,
        "posts"     => (int)$posts
    ]
]);
?>
