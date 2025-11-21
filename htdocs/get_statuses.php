<?php
require "db.php";
require "helpers.php";

$ids_param = isset($_GET['user_ids']) ? $_GET['user_ids'] : null;
if (!$ids_param) {
    $raw = file_get_contents('php://input');
    $d = json_decode($raw, true);
    if ($d && isset($d['user_ids'])) $ids_param = $d['user_ids'];
}

if (!$ids_param) json_response(["success"=>false,"message"=>"user_ids required (comma separated or json array)"]);

// normalize to array
if (is_string($ids_param)) $ids_param = explode(',', $ids_param);
if (!is_array($ids_param)) json_response(["success"=>false,"message"=>"user_ids must be array or comma separated string"]);

$ids = array_map('intval', $ids_param);
$placeholders = implode(',', array_fill(0, count($ids), '?'));
$types = str_repeat('i', count($ids));

$sql = "SELECT up.user_id, up.status, up.last_seen, up.updated_at, u.username, u.imageBase64 FROM user_presence up RIGHT JOIN users u ON u.id = up.user_id WHERE up.user_id IN ($placeholders) OR u.id IN ($placeholders)";

$stmt = $conn->prepare($sql);
$params = array_merge($ids, $ids);
$refs = [];
$types_all = $types . $types;
$refs[] = & $types_all;
foreach ($params as $k => $v) { $refs[] = & $params[$k]; }
call_user_func_array(array($stmt, 'bind_param'), $refs);
$stmt->execute(); $res = $stmt->get_result();
$out = [];
while ($r = $res->fetch_assoc()) {
    $uid = (int)$r['user_id'];
    $out[$uid] = [
        'userId' => $uid,
        'username' => $r['username'],
        'profileImage' => $r['imageBase64'],
        'status' => $r['status'] ? $r['status'] : 'offline',
        'last_seen' => $r['last_seen']
    ];
}

json_response(["success"=>true, "statuses"=>$out]);
?>
