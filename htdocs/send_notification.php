<?php
require_once 'db.php';

function send_notification($user_id, $title, $body) {
    global $conn;
    $stmt = $conn->prepare("SELECT fcm_token FROM fcm_tokens WHERE user_id = ?");
    $stmt->bind_param("i", $user_id);
    $stmt->execute();
    $result = $stmt->get_result();
    $tokens = [];
    while ($row = $result->fetch_assoc()) {
        $tokens[] = $row['fcm_token'];
    }
    $stmt->close();

    if (empty($tokens)) {
        return;
    }

    $url = 'https://fcm.googleapis.com/fcm/send';
    $fields = [
        'registration_ids' => $tokens,
        'notification' => [
            'title' => $title,
            'body' => $body,
        ],
    ];

    $headers = [
        'Authorization: key=YOUR_SERVER_KEY', // Replace with your FCM server key
        'Content-Type: application/json',
    ];

    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
    $result = curl_exec($ch);
    curl_close($ch);
}
?>