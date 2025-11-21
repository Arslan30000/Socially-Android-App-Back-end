<?php
require "db.php";
require "helpers.php";

$token = get_bearer_token();
if (!$token) json_response(["success"=>false,"message"=>"No token provided"]);

$stmt = $conn->prepare("SELECT user_id FROM tokens WHERE token=? AND (expires_at IS NULL OR expires_at > NOW()) LIMIT 1");
if (!$stmt) json_response(["success"=>false,"message"=>"DB prepare failed", "error"=>$conn->error]);
$stmt->bind_param("s", $token);
$stmt->execute();
$stmt->store_result();
if ($stmt->num_rows === 0) json_response(["success"=>false,"message"=>"Invalid token"]);
$stmt->bind_result($current_user_id);
$stmt->fetch();

$q = $_GET['q'] ?? '';
if ($q === '') json_response(["success"=>true,"users"=>[]]);

$q_like = "%" . $q . "%";
$stmt2 = $conn->prepare("SELECT id, username, imageBase64 FROM users WHERE (username LIKE ?) LIMIT 20");
$stmt2->bind_param("s", $q_like);
$stmt2->execute();
$res = $stmt2->get_result();

$users = [];
while ($row = $res->fetch_assoc()) {
    $id = (int)$row['id'];
    if ($id === (int)$current_user_id) continue; // skip self

    $stmtf = $conn->prepare("SELECT 1 FROM followers WHERE follower_id=? AND following_id=? LIMIT 1");
    $stmtf->bind_param("ii", $current_user_id, $id);
    $stmtf->execute();
    $stmtf->store_result();
    $is_following = $stmtf->num_rows > 0;

    $stmtr = $conn->prepare("SELECT 1 FROM follow_requests WHERE from_user_id=? AND to_user_id=? LIMIT 1");
    $stmtr->bind_param("ii", $current_user_id, $id);
    $stmtr->execute();
    $stmtr->store_result();
    $already_requested = $stmtr->num_rows > 0;

    $users[] = [
        "id" => $id,
        "username" => $row['username'],
        "imageBase64" => $row['imageBase64'],
        "is_following" => $is_following,
        "already_requested" => $already_requested
    ];
}

json_response(["success"=>true, "users"=>$users]);
?>