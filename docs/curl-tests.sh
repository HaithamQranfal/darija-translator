#!/usr/bin/env bash
# cURL test suite for the TranslatorResource endpoint.
BASE=${BASE:-http://localhost:8080/translator}
set -x
curl -s "$BASE/api/health"; echo
curl -s -u haitham:darija2026 -H "Content-Type: application/json" \
     -d '{"text":"How are you today?"}' "$BASE/api/translator/translate"; echo
curl -s -u haitham:darija2026 -H "Content-Type: text/plain" \
     --data "I am hungry, where can I eat couscous?" "$BASE/api/translator/translate"; echo
curl -s -u haitham:darija2026 "$BASE/api/translator/translate?text=Good%20night"; echo
curl -s -u haitham:darija2026 "$BASE/api/translator/whoami"; echo
curl -s -i -H "Content-Type: application/json" -d '{"text":"hello"}' "$BASE/api/translator/translate" | head -5   # 401
curl -s -i -u haitham:wrong -H "Content-Type: application/json" -d '{"text":"hello"}' "$BASE/api/translator/translate" | head -5   # 401
curl -s -u haitham:darija2026 -H "Content-Type: application/json" -d '{"text":""}' "$BASE/api/translator/translate"; echo   # 400
