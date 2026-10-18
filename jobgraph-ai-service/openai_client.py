"""Small shared OpenAI Responses API client for the AI service."""

import json
import os
from pathlib import Path

import requests
from dotenv import load_dotenv

load_dotenv(Path(__file__).with_name(".env"))

OPENAI_API_KEY = os.getenv("OPENAI_API_KEY")
OPENAI_MODEL = os.getenv("OPENAI_MODEL", "gpt-6-luna")


def generate_json(prompt: str) -> dict:
    if not OPENAI_API_KEY:
        raise RuntimeError("OPENAI_API_KEY is missing from jobgraph-ai-service/.env")

    response = requests.post(
        "https://api.openai.com/v1/responses",
        headers={"Authorization": f"Bearer {OPENAI_API_KEY}"},
        json={
            "model": OPENAI_MODEL,
            "input": prompt,
            "text": {"format": {"type": "json_object"}},
            "reasoning": {"effort": "none"},
            "store": False,
        },
        timeout=(10, 90),
    )

    if not response.ok:
        error = response.json().get("error", {})
        message = error.get("message", response.reason)
        raise RuntimeError(f"OpenAI API returned HTTP {response.status_code}: {message}")

    data = response.json()
    if data.get("status") != "completed":
        raise RuntimeError("OpenAI did not complete the response")

    for item in data.get("output", []):
        for content in item.get("content", []):
            if content.get("type") == "output_text":
                return json.loads(content["text"])

    raise RuntimeError("OpenAI response did not contain JSON output")
