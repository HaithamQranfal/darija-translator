"""Desktop GUI (Tkinter) for the Darija Translator REST service.

Run:  python gui_app.py
Optional read-aloud:  pip install pyttsx3
"""
from __future__ import annotations

import threading
import tkinter as tk
from tkinter import messagebox, ttk

from translator_client import TranslatorClient, TranslatorError

try:
    import pyttsx3  # optional text-to-speech
except ImportError:  # pragma: no cover
    pyttsx3 = None

RED, GREEN = "#C1272D", "#006233"


class App(tk.Tk):
    def __init__(self) -> None:
        super().__init__()
        self.title("Darija Translator - Python client")
        self.geometry("640x560")
        self.minsize(480, 420)
        self.client = TranslatorClient.from_env()
        self.last = None
        self._build()

    def _build(self) -> None:
        header = tk.Frame(self, bg=RED)
        header.pack(fill="x")
        tk.Label(header, text="Darija Translator", bg=RED, fg="white",
                 font=("Segoe UI", 16, "bold"), pady=10, padx=14).pack(side="left")

        body = ttk.Frame(self, padding=14)
        body.pack(fill="both", expand=True)

        conn = ttk.Frame(body)
        conn.pack(fill="x")
        ttk.Label(conn, text="Server").pack(side="left")
        self.url = tk.StringVar(value=self.client.base_url)
        ttk.Entry(conn, textvariable=self.url).pack(side="left", fill="x", expand=True, padx=6)
        self.user = tk.StringVar(value="haitham")
        self.pwd = tk.StringVar(value="darija2026")
        ttk.Entry(conn, textvariable=self.user, width=10).pack(side="left")
        ttk.Entry(conn, textvariable=self.pwd, width=12, show="*").pack(side="left", padx=(6, 0))

        row = ttk.Frame(body)
        row.pack(fill="x", pady=(12, 4))
        ttk.Label(row, text="Source").pack(side="left")
        self.source = ttk.Combobox(row, values=["auto", "English", "French", "Spanish"], width=12, state="readonly")
        self.source.set("auto")
        self.source.pack(side="left", padx=6)

        self.input = tk.Text(body, height=7, wrap="word", font=("Segoe UI", 11))
        self.input.pack(fill="x")
        self.input.bind("<Control-Return>", lambda e: (self.translate(), "break"))

        btns = ttk.Frame(body)
        btns.pack(fill="x", pady=8)
        self.btn = tk.Button(btns, text="Translate to Darija  (Ctrl+Enter)", bg=GREEN, fg="white",
                             font=("Segoe UI", 11, "bold"), relief="flat", padx=12, pady=6, command=self.translate)
        self.btn.pack(side="left")
        ttk.Button(btns, text="Read aloud", command=self.speak).pack(side="left", padx=6)
        ttk.Button(btns, text="Copy", command=self.copy).pack(side="left")

        self.output = tk.Label(body, text="", font=("Segoe UI", 20), wraplength=580, justify="right", anchor="e")
        self.output.pack(fill="x", pady=(10, 2))
        self.latin = tk.Label(body, text="", font=("Segoe UI", 11, "italic"), fg="#57534e", wraplength=580, anchor="w", justify="left")
        self.latin.pack(fill="x")
        self.status = tk.Label(body, text="", fg="#78716c", anchor="w")
        self.status.pack(fill="x", side="bottom")

    def translate(self) -> None:
        text = self.input.get("1.0", "end").strip()
        if not text:
            return
        self.client = TranslatorClient(self.url.get(), self.user.get(), self.pwd.get())
        self.btn.config(state="disabled")
        self.status.config(text="Translating...", fg="#78716c")
        threading.Thread(target=self._worker, args=(text, self.source.get()), daemon=True).start()

    def _worker(self, text: str, source: str) -> None:
        try:
            result = self.client.translate(text, source)
            self.after(0, self._show, result)
        except TranslatorError as e:
            self.after(0, self._error, str(e))

    def _show(self, r) -> None:
        self.last = r
        self.btn.config(state="normal")
        self.output.config(text=r.translation)
        self.latin.config(text=r.transliteration or "")
        self.status.config(text=f"{r.source_language} -> Darija | {r.provider}/{r.model} | {r.duration_ms} ms", fg="#78716c")

    def _error(self, msg: str) -> None:
        self.btn.config(state="normal")
        self.status.config(text=msg, fg=RED)

    def copy(self) -> None:
        if self.last:
            self.clipboard_clear()
            self.clipboard_append(self.last.translation)
            self.status.config(text="Copied")

    def speak(self) -> None:
        if not self.last:
            return
        if pyttsx3 is None:
            messagebox.showinfo("Read aloud", "Install pyttsx3 for text-to-speech:  pip install pyttsx3")
            return

        def run():
            engine = pyttsx3.init()
            arabic = next((v for v in engine.getProperty("voices")
                           if "ar" in str(getattr(v, "languages", "")).lower() or "arab" in v.name.lower()), None)
            if arabic:
                engine.setProperty("voice", arabic.id)
                engine.say(self.last.translation)
            else:  # no Arabic voice installed: read the Latin transliteration
                engine.say(self.last.transliteration or self.last.translation)
            engine.runAndWait()

        threading.Thread(target=run, daemon=True).start()


if __name__ == "__main__":
    App().mainloop()
