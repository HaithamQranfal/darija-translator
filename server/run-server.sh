#!/usr/bin/env bash
# Builds the WAR and starts it on the embedded Payara Micro container (http://localhost:8080/translator).
export LLM_PROVIDER="${LLM_PROVIDER:-gemini}"
: "${GEMINI_API_KEY:?Set GEMINI_API_KEY (https://aistudio.google.com/apikey) or run with LLM_PROVIDER=ollama|mock}"
mvn clean package payara-micro:start
