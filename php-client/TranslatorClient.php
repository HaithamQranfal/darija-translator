<?php
declare(strict_types=1);

/**
 * PHP client for the TranslatorResource REST endpoint (HTTP Basic authentication, JSON).
 */
final class TranslatorClient
{
    public function __construct(
        private string $baseUrl,
        private string $username,
        private string $password,
        private int $timeout = 60
    ) {
        $this->baseUrl = rtrim($baseUrl, '/');
    }

    /** Calls translate(): returns the decoded TranslationResponse array. */
    public function translate(string $text, string $sourceLanguage = 'auto'): array
    {
        return $this->request('POST', '/api/translator/translate', [
            'text' => $text,
            'sourceLanguage' => $sourceLanguage,
        ]);
    }

    public function health(): array
    {
        return $this->request('GET', '/api/health');
    }

    private function request(string $method, string $path, ?array $body = null): array
    {
        $ch = curl_init($this->baseUrl . $path);
        $headers = ['Accept: application/json', 'X-Requested-With: DarijaTranslatorPHP'];
        $options = [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CUSTOMREQUEST  => $method,
            CURLOPT_HTTPAUTH       => CURLAUTH_BASIC,
            CURLOPT_USERPWD        => $this->username . ':' . $this->password,
            CURLOPT_TIMEOUT        => $this->timeout,
            CURLOPT_CONNECTTIMEOUT => 5,
        ];
        if ($body !== null) {
            $headers[] = 'Content-Type: application/json; charset=UTF-8';
            $options[CURLOPT_POSTFIELDS] = json_encode($body, JSON_UNESCAPED_UNICODE);
        }
        $options[CURLOPT_HTTPHEADER] = $headers;
        curl_setopt_array($ch, $options);

        $raw = curl_exec($ch);
        if ($raw === false) {
            $err = curl_error($ch);
            curl_close($ch);
            throw new RuntimeException("Cannot reach {$this->baseUrl}: $err");
        }
        $status = (int) curl_getinfo($ch, CURLINFO_RESPONSE_CODE);
        curl_close($ch);

        $data = json_decode($raw, true);
        if ($status === 401) {
            throw new RuntimeException('Wrong username or password (401)');
        }
        if ($status >= 400 || !is_array($data)) {
            $msg = is_array($data) && isset($data['message']) ? $data['message'] : $raw;
            throw new RuntimeException("HTTP $status: $msg");
        }
        return $data;
    }
}
