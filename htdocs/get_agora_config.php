<?php
header('Content-Type: application/json');

// This script provides the necessary configuration for the Agora client.
// Using a server-side script for this is more secure than hardcoding keys on the client.

$config = [
    "success" => true,
    "agora_app_id" => "4e2534f9f9dc48b98e9f2153a207dcf8"
    // In a production environment, you would also generate and send a token here.
    // "token" => generateAgoraToken($channelName, $userId)
];

echo json_encode($config);
?>
