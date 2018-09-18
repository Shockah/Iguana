<?php

if (!isset($_GET['nick']) || !isset($_GET['backgroundRgb']) || !isset($_GET['textRgb'])) {
	http_response_code(503);
	die();
}

require __DIR__ . '/vendor/autoload.php';

$avatar = new LasseRafn\InitialAvatarGenerator\InitialAvatar();
$avatar = $avatar->size(256);
$avatar = $avatar->length(3);
$avatar = $avatar->fontSize(0.4);

$avatar = $avatar->name($_GET['nick']);
$avatar = $avatar->background('#' . $_GET['backgroundRgb']);
$avatar = $avatar->color('#' . $_GET['textRgb']);

echo $avatar->generate()->response('png', 100);

?>