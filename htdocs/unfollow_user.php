<?php
require_once 'db.php';
require_once 'validate_tokens.php';

if ($_SERVER['REQUEST_METHOD'] == 'POST') {
    $from_user_id = validate_token();
    if (!$from_user_id) {
        echo json_encode(["success" => false, "message" => "Unauthorized"]);
        exit;
    }

    $to_user_id = $_POST['to_user_id'] ?? 0;

    if (!$to_user_id) {
        echo json_encode(["success" => false, "message" => "User ID required"]);
        exit;
    }

    // Check if follow relationship exists
    $checkStmt = $conn->prepare("SELECT id FROM followers WHERE follower_id = ? AND following_id = ?");
    $checkStmt->bind_param("ii", $from_user_id, $to_user_id);
    $checkStmt->execute();
    
    if ($checkStmt->get_result()->num_rows == 0) {
        echo json_encode(["success" => false, "message" => "Not following this user"]);
        $checkStmt->close();
        exit;
    }
    $checkStmt->close();

    // Delete the follow relationship
    $stmt = $conn->prepare("DELETE FROM followers WHERE follower_id = ? AND following_id = ?");
    $stmt->bind_param("ii", $from_user_id, $to_user_id);
    
    if ($stmt->execute()) {
        // recompute counts for the target user
        $uid = (int)$to_user_id;
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

        echo json_encode(["success" => true, "message" => "Unfollowed successfully", "to_user_counts" => ["followers" => (int)$followersCount, "following" => (int)$followingCount, "posts" => (int)$postsCount]]);
    } else {
        echo json_encode(["success" => false, "message" => "Unfollow failed"]);
    }
    $stmt->close();
} else {
    echo json_encode(["success" => false, "message" => "Invalid request method"]);
}
?>
