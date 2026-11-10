<?php
// Connection settings for the Darija Translator REST service (override with environment variables).
return [
    'server_url' => getenv('TRANSLATOR_URL') ?: 'http://localhost:8080/translator',
    'username'   => getenv('TRANSLATOR_USER') ?: 'haitham',
    'password'   => getenv('TRANSLATOR_PASSWORD') ?: 'darija2026',
    'timeout'    => 60,
];
