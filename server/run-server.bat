@echo off
REM Builds the WAR and starts it on the embedded Payara Micro container (http://localhost:8080/translator).
REM 1) Put your free Gemini key below (https://aistudio.google.com/apikey) or use LLM_PROVIDER=ollama / mock.
if "%GEMINI_API_KEY%"=="" set GEMINI_API_KEY=PUT_YOUR_KEY_HERE
if "%LLM_PROVIDER%"=="" set LLM_PROVIDER=gemini
call mvn clean package payara-micro:start
