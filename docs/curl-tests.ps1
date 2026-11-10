# PowerShell version (Windows). Note: use curl.exe, not the Invoke-WebRequest alias.
$BASE = "http://localhost:8080/translator"
curl.exe -s "$BASE/api/health"
curl.exe -s -u haitham:darija2026 -H "Content-Type: application/json" -d '{\"text\":\"How are you today?\"}' "$BASE/api/translator/translate"
curl.exe -s -u haitham:darija2026 "$BASE/api/translator/whoami"
curl.exe -s -i -H "Content-Type: application/json" -d '{\"text\":\"hello\"}' "$BASE/api/translator/translate"
