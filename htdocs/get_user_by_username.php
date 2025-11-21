<?php
require_once 'db.php';
require_once 'validate_tokens.php';

// GET ?username=...
$username = $_GET['username'] ?? '';
if (empty($username)) {
    echo json_encode(["success" => false, "message" => "username required"]);
    exit;
}

$stmt = $conn->prepare("SELECT id, username, name, bio, email, phone, gender, imageBase64 FROM users WHERE username = ? LIMIT 1");
$stmt->bind_param("s", $username);
$stmt->execute();
$res = $stmt->get_result();
if ($res->num_rows == 0) {
    echo json_encode(["success" => false, "message" => "User not found"]);
    exit;
}
$user = $res->fetch_assoc();

// counts
$uid = (int)$user['id'];
$followersCount = 0; $followingCount = 0; $postsCount = 0;
$rc = $conn->prepare("SELECT COUNT(*) as c FROM followers WHERE following_id = ?");
$rc->bind_param("i", $uid); $rc->execute(); $rres = $rc->get_result(); if ($rres) { $followersCount = $rres->fetch_assoc()['c']; }
$rc->close();
$rc = $conn->prepare("SELECT COUNT(*) as c FROM followers WHERE follower_id = ?");
$rc->bind_param("i", $uid); $rc->execute(); $rres = $rc->get_result(); if ($rres) { $followingCount = $rres->fetch_assoc()['c']; }
$rc->close();
$rc = $conn->prepare("SELECT COUNT(*) as c FROM posts WHERE user_id = ?");
$rc->bind_param("i", $uid); $rc->execute(); $rres = $rc->get_result(); if ($rres) { $postsCount = $rres->fetch_assoc()['c']; }
$rc->close();

// relationship if token provided
$relationship = new stdClass();
$relationship->is_following = false;
$relationship->has_requested = false;

$tokenUser = null;
try {
    $tokenUser = validate_token();
} catch (Exception $e) { $tokenUser = null; }

if ($tokenUser) {
    $chk = $conn->prepare("SELECT id FROM followers WHERE follower_id = ? AND following_id = ?");
    $chk->bind_param("ii", $tokenUser, $uid); $chk->execute();
    $rr = $chk->get_result();
    if ($rr && $rr->num_rows > 0) $relationship->is_following = true;
    $chk->close();

    $chk = $conn->prepare("SELECT id FROM follow_requests WHERE from_user_id = ? AND to_user_id = ?");
    $chk->bind_param("ii", $tokenUser, $uid); $chk->execute();
    $rr = $chk->get_result();
    if ($rr && $rr->num_rows > 0) $relationship->has_requested = true;
    $chk->close();
}

echo json_encode([
    "success" => true,
    "user" => [
        "id" => (int)$user['id'],
        "username" => $user['username'],
        "name" => $user['name'],
        "bio" => $user['bio'],
        "email" => $user['email'],
        "phone" => $user['phone'],
        "gender" => $user['gender'],
        "imageBase64" => $user['imageBase64']
    ],
    "counts" => ["followers" => (int)$followersCount, "following" => (int)$followingCount, "posts" => (int)$postsCount],
    "relationship" => $relationship
]);

?>