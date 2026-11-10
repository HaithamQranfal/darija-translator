<?php
declare(strict_types=1);
require __DIR__ . '/TranslatorClient.php';
$config = require __DIR__ . '/config.php';

$client = new TranslatorClient($config['server_url'], $config['username'], $config['password'], $config['timeout']);
$text = trim($_POST['text'] ?? '');
$source = $_POST['source'] ?? 'auto';
$result = null;
$error = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST' && $text !== '') {
    try {
        $result = $client->translate($text, $source);
    } catch (RuntimeException $e) {
        $error = $e->getMessage();
    }
}
$h = fn(?string $s): string => htmlspecialchars((string) $s, ENT_QUOTES, 'UTF-8');
$sources = ['auto' => 'Detect language', 'English' => 'English', 'French' => 'French', 'Spanish' => 'Spanish'];
?>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Darija Translator — PHP client</title>
    <style>
        body { font-family: system-ui, "Segoe UI", sans-serif; background: #fafaf9; color: #1c1917; margin: 0; }
        header { background: #c1272d; color: #fff; padding: 16px 24px; }
        header h1 { margin: 0; font-size: 20px; }
        main { max-width: 720px; margin: 24px auto; padding: 0 16px; }
        textarea, select { width: 100%; font: inherit; padding: 10px; border: 1px solid #d6d3d1; border-radius: 8px; box-sizing: border-box; }
        button { background: #006233; color: #fff; border: 0; border-radius: 8px; padding: 12px 20px; font: inherit; font-weight: 600; cursor: pointer; margin-top: 10px; }
        .card { background: #fff; border: 1px solid #e7e5e4; border-radius: 10px; padding: 16px; margin-top: 18px; }
        .ar { font-size: 26px; direction: rtl; line-height: 1.6; margin: 0; }
        .latin { font-style: italic; color: #57534e; }
        .meta { font-size: 12px; color: #78716c; }
        .error { color: #c1272d; }
    </style>
</head>
<body>
<header><h1>Darija Translator <small>· PHP client</small></h1></header>
<main>
    <form method="post">
        <label for="source">Source language</label>
        <select id="source" name="source">
            <?php foreach ($sources as $value => $label): ?>
                <option value="<?= $h($value) ?>" <?= $value === $source ? 'selected' : '' ?>><?= $h($label) ?></option>
            <?php endforeach; ?>
        </select>
        <p><label for="text">Text</label>
        <textarea id="text" name="text" rows="5" required placeholder="Type the text to translate..."><?= $h($text) ?></textarea></p>
        <button type="submit">Translate to Darija</button>
    </form>

    <?php if ($error): ?>
        <p class="card error"><?= $h($error) ?></p>
    <?php elseif ($result): ?>
        <div class="card">
            <p class="ar"><?= $h($result['translation']) ?></p>
            <?php if (!empty($result['transliteration'])): ?>
                <p class="latin"><?= $h($result['transliteration']) ?></p>
            <?php endif; ?>
            <button type="button" onclick="speak()">🔊 Read aloud</button>
            <p class="meta"><?= $h($result['sourceLanguage']) ?> → Darija · <?= $h($result['provider']) ?>/<?= $h($result['model']) ?> · <?= (int) $result['durationMs'] ?> ms</p>
        </div>
        <script>
            function speak() {
                const ar = <?= json_encode($result['translation'], JSON_UNESCAPED_UNICODE) ?>;
                const latin = <?= json_encode($result['transliteration'] ?? '', JSON_UNESCAPED_UNICODE) ?>;
                const voices = speechSynthesis.getVoices();
                const v = voices.find(x => x.lang.toLowerCase().startsWith('ar'));
                const u = new SpeechSynthesisUtterance(v ? ar : (latin || ar));
                if (v) { u.voice = v; u.lang = v.lang; } else { u.lang = 'fr-FR'; }
                speechSynthesis.speak(u);
            }
        </script>
    <?php endif; ?>
</main>
</body>
</html>
