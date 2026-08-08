"""
ProcureAI FastAPI AI Service — Configuration
Reads from environment variables with safe defaults.
"""
import os
from dataclasses import dataclass


@dataclass
class Settings:
    gemini_api_key: str = ""
    gemini_model: str = "gemini-1.5-flash-latest"
    gemini_base_url: str = "https://generativelanguage.googleapis.com/v1beta/models/"
    max_input_chars: int = 40_000
    request_timeout_s: int = 45
    demo_mode: bool = False  # auto-detected: True when gemini_api_key is empty


def load_settings() -> Settings:
    key = os.environ.get("GEMINI_API_KEY", os.environ.get("AI_API_KEY", "")).strip()
    model = os.environ.get("GEMINI_MODEL", "gemini-1.5-flash-latest").strip()
    s = Settings(
        gemini_api_key=key,
        gemini_model=model if model else "gemini-1.5-flash-latest",
        max_input_chars=int(os.environ.get("AI_MAX_INPUT_CHARS", "40000")),
        request_timeout_s=int(os.environ.get("AI_REQUEST_TIMEOUT_S", "45")),
    )
    s.demo_mode = not bool(s.gemini_api_key)
    return s


settings: Settings = load_settings()
