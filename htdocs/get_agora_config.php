<?php
require "helpers.php";

// This script provides the Agora App ID to the client.
// In a production environment, you might also generate a token here,
// but for development, providing the App ID is sufficient for token-less auth.

$agora_app_id = "4e2534f9f9dc48b98e9f2153a207dcf8";

json_response([
    "success" => true,
    "app_id" => $agora_app_id
]);
?>
