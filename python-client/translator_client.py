"""Python client for the Darija Translator REST service (TranslatorResource).

Standard library only. Usage:
    python translator_client.py "How are you today?"
    python translator_client.py --interactive
    python translator_client.py --source French "Bonjour, ça va ?"

Settings come from environment variables TRANSLATOR_URL, TRANSLATOR_USER, TRANSLATOR_PASSWORD.
"""
from __future__ import annotations

import argparse
import base64
import json
import os
import sys
import urllib.error
import urllib.request
from dataclasses import dataclass
from typing import Optional

DEFAULT_URL = "http://localhost:8080/translator"


class TranslatorError(Exception):
    """Raised for network, authentication or server errors."""


@dataclass
class Translation:
    original: str
    translation: str
    transliteration: Optional[str]
    source_language: str
    provider: str
    model: str
    duration_ms: int
    cached: bool

    @classmethod
    def from_json(cls, d: dict) -> "Translation":
        return cls(
            original=d.get("original", ""),
            translation=d.get("translation", ""),
            transliteration=d.get("transliteration"),
            source_language=d.get("sourceLanguage", "auto"),
            provider=d.get("provider", "?"),
            model=d.get("model", "?"),
            duration_ms=int(d.get("durationMs", 0)),
            cached=bool(d.get("cached", False)),
        )


class TranslatorClient:
    def __init__(self, base_url: str = DEFAULT_URL, username: str = "haitham",
                 password: str = "darija2026", timeout: float = 60):
        self.base_url = base_url.rstrip("/")
        self.timeout = timeout
        token = base64.b64encode(f"{username}:{password}".encode("utf-8")).decode("ascii")
        self._headers = {
            "Authorization": f"Basic {token}",
            "Accept": "application/json",
            "X-Requested-With": "DarijaTranslatorPython",
        }

    @classmethod
    def from_env(cls) -> "TranslatorClient":
        return cls(os.getenv("TRANSLATOR_URL", DEFAULT_URL),
                   os.getenv("TRANSLATOR_USER", "haitham"),
                   os.getenv("TRANSLATOR_PASSWORD", "darija2026"))

    def _request(self, method: str, path: str, body: Optional[dict] = None) -> dict:
        data = None
        headers = dict(self._headers)
        if body is not None:
            data = json.dumps(body, ensure_ascii=False).encode("utf-8")
            headers["Content-Type"] = "application/json; charset=UTF-8"
        req = urllib.request.Request(self.base_url + path, data=data, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                return json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            if e.code == 401:
                raise TranslatorError("Wrong username or password (401)") from None
            try:
                message = json.loads(e.read().decode("utf-8")).get("message", e.reason)
            except (ValueError, AttributeError):
                message = e.reason
            raise TranslatorError(f"HTTP {e.code}: {message}") from None
        except urllib.error.URLError as e:
            raise TranslatorError(f"Cannot reach {self.base_url}: {e.reason}") from None

    def translate(self, text: str, source_language: str = "auto") -> Translation:
        """Calls the translate() business method of TranslatorResource."""
        return Translation.from_json(self._request(
            "POST", "/api/translator/translate", {"text": text, "sourceLanguage": source_language}))

    def health(self) -> dict:
        return self._request("GET", "/api/health")


def _print(t: Translation) -> None:
    print(t.translation)
    if t.transliteration:
        print(f"({t.transliteration})")
    print(f"  [{t.source_language} -> Darija | {t.provider}/{t.model} | {t.duration_ms} ms{' cached' if t.cached else ''}]")


def main(argv: Optional[list[str]] = None) -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")  # Arabic output on Windows consoles
    p = argparse.ArgumentParser(description="Translate text to Moroccan Darija via the REST service.")
    p.add_argument("text", nargs="*", help="text to translate")
    p.add_argument("-s", "--source", default="auto", help="source language (default: auto-detect)")
    p.add_argument("-i", "--interactive", action="store_true", help="interactive mode")
    p.add_argument("--url", default=os.getenv("TRANSLATOR_URL", DEFAULT_URL))
    p.add_argument("-u", "--user", default=os.getenv("TRANSLATOR_USER", "haitham"))
    p.add_argument("-p", "--password", default=os.getenv("TRANSLATOR_PASSWORD", "darija2026"))
    args = p.parse_args(argv)

    client = TranslatorClient(args.url, args.user, args.password)
    try:
        if args.interactive or not args.text:
            print("Darija Translator - type text and press Enter (empty line to quit)")
            while True:
                line = input("> ").strip()
                if not line:
                    return 0
                try:
                    _print(client.translate(line, args.source))
                except TranslatorError as e:
                    print(f"Error: {e}", file=sys.stderr)
        _print(client.translate(" ".join(args.text), args.source))
        return 0
    except TranslatorError as e:
        print(f"Error: {e}", file=sys.stderr)
        return 2
    except (KeyboardInterrupt, EOFError):
        return 0


if __name__ == "__main__":
    sys.exit(main())
