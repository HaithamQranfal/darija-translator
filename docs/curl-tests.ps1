# cURL test suite for the TranslatorResource endpoint (Windows PowerShell).
# Uses curl.exe, not the Invoke-WebRequest alias. Each case checks the HTTP status code.
# Usage:  powershell -ExecutionPolicy Bypass -File docs\curl-tests.ps1  [-Base http://host:8080/translator]
param([string]$Base = $(if ($env:BASE) { $env:BASE } else { "http://localhost:8080/translator" }))

$OutputEncoding = [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$script:failures = 0
$bodyFile = [System.IO.Path]::GetTempFileName()

function Test-Case([string]$Name, [int]$Expected, [string[]]$CurlArgs) {
    $code = & curl.exe -s -o $bodyFile -w "%{http_code}" @CurlArgs
    $body = Get-Content -Raw -Encoding UTF8 $bodyFile
    $ok = ([int]$code -eq $Expected)
    if (-not $ok) { $script:failures++ }
    $tag = if ($ok) { "PASS" } else { "FAIL" }
    Write-Host ("[{0}] {1,-28} expected {2}, got {3}" -f $tag, $Name, $Expected, $code) -ForegroundColor $(if ($ok) { "Green" } else { "Red" })
    if ($body) { Write-Host ("       " + $body.Trim()) }
}

# JSON bodies go through files so PowerShell 5.1 does not strip the inner quotes.
function New-JsonFile([string]$Json) {
    $f = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($f, $Json, (New-Object System.Text.UTF8Encoding $false))
    return $f
}
$hello  = New-JsonFile '{"text":"How are you today?"}'
$plain  = New-JsonFile '{"text":"hello"}'
$empty  = New-JsonFile '{"text":""}'
$url    = "$Base/api/translator/translate"
$json   = @("-H", "Content-Type: application/json")

Test-Case "health"                200 @("$Base/api/health")
Test-Case "translate (JSON)"      200 (@("-u", "haitham:darija2026") + $json + @("--data-binary", "@$hello", $url))
Test-Case "translate (text/plain)" 200 @("-u", "haitham:darija2026", "-H", "Content-Type: text/plain", "--data", "I am hungry, where can I eat couscous?", $url)
Test-Case "translate (GET)"       200 @("-u", "haitham:darija2026", "$url`?text=Good%20night")
Test-Case "whoami"                200 @("-u", "haitham:darija2026", "$Base/api/translator/whoami")
Test-Case "no credentials"        401 ($json + @("--data-binary", "@$plain", $url))
Test-Case "wrong password"        401 (@("-u", "haitham:wrong") + $json + @("--data-binary", "@$plain", $url))
Test-Case "empty text"            400 (@("-u", "haitham:darija2026") + $json + @("--data-binary", "@$empty", $url))

Remove-Item $bodyFile, $hello, $plain, $empty -ErrorAction SilentlyContinue
if ($script:failures -gt 0) { Write-Host "$($script:failures) test(s) failed" -ForegroundColor Red; exit 1 }
Write-Host "All tests passed" -ForegroundColor Green
