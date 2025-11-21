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
$stmt->bind_result($current_user_id);
$stmt->fetch();

$profile_user_id = intval($_GET['user_id'] ?? $current_user_id);

// basic user
$stmt2 = $conn->prepare("SELECT id, username, name, lastname, dob, email, imageBase64, bio FROM users WHERE id = ? LIMIT 1");
$stmt2->bind_param("i", $profile_user_id);
$stmt2->execute();
$res = $stmt2->get_result();
if ($res->num_rows === 0) json_response(["success"=>false,"message"=>"User not found"]);
$user = $res->fetch_assoc();

// counts
$stmtc = $conn->prepare("SELECT COUNT(*) as cnt FROM followers WHERE following_id = ?");
$stmtc->bind_param("i", $profile_user_id);
$stmtc->execute();
$rc = $stmtc->get_result()->fetch_assoc();
$followers_count = intval($rc['cnt']);

$stmtc2 = $conn->prepare("SELECT COUNT(*) as cnt FROM followers WHERE follower_id = ?");
$stmtc2->bind_param("i", $profile_user_id);
$stmtc2->execute();
$rc2 = $stmtc2->get_result()->fetch_assoc();
$following_count = intval($rc2['cnt']);

$stmtp = $conn->prepare("SELECT COUNT(*) as cnt FROM posts WHERE user_id = ?");
$stmtp->bind_param("i", $profile_user_id);
$stmtp->execute();
$rp = $stmtp->get_result()->fetch_assoc();
$posts_count = intval($rp['cnt']);

// relationship with current user
$isst_following = false;
$stmtf = $conn->prepare("SELECT 1 FROM followers WHERE follower_id=? AND following_id=? LIMIT 1");
$stmtf->bind_param("ii", $current_user_id, $profile_user_id);
$stmtf->execute();
$stmtf->store_result();
$isst_following = $stmtf->num_rows > 0;

$has_requested = false;
$stmtr = $conn->prepare("SELECT 1 FROM follow_requests WHERE from_user_id=? AND to_user_id=? LIMIT 1");
$stmtr->bind_param("ii", $current_user_id, $profile_user_id);
$stmtr->execute();
$stmtr->store_result();
$has_requested = $stmtr->num_rows > 0;

$json = [
    "success" => true,
    "user" => [
        "id" => (int)$user['id'],
        "username" => $user['username'],
        "name" => $user['name'],
        "lastname" => $user['lastname'],
        "dob" => $user['dob'],
        "email" => $user['email'],
        "imageBase64" => $user['imageBase64'],
        "bio" => $user['bio']
    ],
    "counts" => [
        "followers" => $followers_count,
        "following" => $following_count,
        "posts" => $posts_count
    ],
    "relationship" => [
        "is_following" => $isst_following,
        "has_requested" => $has_requested
    ]
];

json_response($json);
?>