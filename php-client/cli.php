<?php
// Usage: php cli.php "How are you?"
declare(strict_types=1);
require __DIR__ . '/TranslatorClient.php';
$config = require __DIR__ . '/config.php';

$text = implode(' ', array_slice($argv, 1)) ?: trim((string) stream_get_contents(STDIN));
if ($text === '') {
    fwrite(STDERR, "Usage: php cli.php \"text to translate\"\n");
    exit(1);
}
try {
    $client = new TranslatorClient($config['server_url'], $config['username'], $config['password']);
    $r = $client->translate($text);
    echo $r['translation'], PHP_EOL;
    if (!empty($r['transliteration'])) echo '(', $r['transliteration'], ')', PHP_EOL;
} catch (RuntimeException $e) {
    fwrite(STDERR, 'Error: ' . $e->getMessage() . PHP_EOL);
    exit(2);
}
