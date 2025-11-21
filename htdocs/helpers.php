<?php
function json_response($arr) {
    echo json_encode($arr);
    exit;
}

function generate_token($len=64) {
    return bin2hex(random_bytes($len/2));
}

function get_bearer_token() {
    $headers = null;
    if (function_exists('apache_request_headers')) $headers = apache_request_headers();
    if ($headers && isset($headers['Authorization'])) {
        $parts = explode(' ', $headers['Authorization']);
        if (count($parts) === 2) return $parts[1];
    }
    if (isset($_SERVER['HTTP_AUTHORIZATION'])) {
        $parts = explode(' ', $_SERVER['HTTP_AUTHORIZATION']);
        if (count($parts) === 2) return $parts[1];
    }
    return null;
}
?>
