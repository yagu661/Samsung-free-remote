"""
Ultra-Resilient LLM Multi-Provider Fallback Engine
==================================================

Production-ready, highly resilient LLM fallback system in Python that cycles
through 6 major free/freemium AI API providers (13 models total) in a strict priority order.

If any provider or specific model fails due to:
  - Rate limits (HTTP 429)
  - Server errors (HTTP 500, 502, 503, 504)
  - Timeouts (per-request 8-10s)
  - Invalid credentials / Authentication errors (HTTP 401, 403)
  - Network connection errors

The system catches the exception, prints a clean warning log, and immediately
retries with the next model/provider in the fallback chain.

Strict Fallback Chain (13 Endpoints):
------------------------------------
 Tier 1 (Google AI Studio):
   1. gemini-2.0-flash
   2. gemini-1.5-flash
 Tier 2 (GroqCloud):
   3. llama-3.3-70b-versatile
   4. llama-3.1-8b-instant
   5. mixtral-8x7b-32768
 Tier 3 (Cerebras Cloud):
   6. llama3.1-8b
   7. llama3.3-70b
 Tier 4 (OpenRouter - Universal Gateway):
   8. openrouter/free
   9. qwen/qwen3.8-27b:free
   10. nvidia/nemotron-3.5-lightning:free
 Tier 5 (Mistral AI):
   11. mistral-small-latest
   12. open-mistral-7b
 Tier 6 (Cloudflare Workers AI):
   13. @cf/meta/llama-3.1-8b-instruct

Author: Senior Python Engineer for Samsung Smart TV AI Assistant
"""

from __future__ import annotations

import os
import sys
import time
import logging
from dataclasses import dataclass
from enum import Enum
from typing import List, Optional, Callable, Dict, Any

# Configure clean logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    datefmt="%H:%M:%S",
)
logger = logging.getLogger("LLMFallback")

# Per-request timeout in seconds (stops hanging requests immediately)
REQUEST_TIMEOUT: float = 10.0


class ProviderType(str, Enum):
    GOOGLE = "Google AI Studio"
    GROQ = "GroqCloud"
    CEREBRAS = "Cerebras Cloud"
    OPENROUTER = "OpenRouter"
    MISTRAL = "Mistral AI"
    CLOUDFLARE = "Cloudflare Workers AI"


@dataclass
class ModelEndpoint:
    """Represents a single model target in the fallback chain."""
    tier: int
    provider: ProviderType
    model_name: str
    api_key_env_var: str
    base_url: Optional[str] = None
    account_id_env_var: Optional[str] = None

    @property
    def api_key(self) -> Optional[str]:
        val = os.getenv(self.api_key_env_var, "").strip()
        return val if val else None

    @property
    def account_id(self) -> Optional[str]:
        if not self.account_id_env_var:
            return None
        val = os.getenv(self.account_id_env_var, "").strip()
        return val if val else None

    def get_effective_base_url(self) -> Optional[str]:
        if self.provider == ProviderType.CLOUDFLARE:
            acc_id = self.account_id
            if not acc_id:
                return None
            return f"https://api.cloudflare.com/client/v4/accounts/{acc_id}/ai/v1"
        return self.base_url


def build_fallback_chain() -> List[ModelEndpoint]:
    """
    Constructs the exact 13-model fallback chain in strict priority order.
    """
    return [
        # --- Tier 1: Google AI Studio ---
        ModelEndpoint(
            tier=1,
            provider=ProviderType.GOOGLE,
            model_name="gemini-2.0-flash",
            api_key_env_var="GEMINI_API_KEY",
        ),
        ModelEndpoint(
            tier=1,
            provider=ProviderType.GOOGLE,
            model_name="gemini-1.5-flash",
            api_key_env_var="GEMINI_API_KEY",
        ),

        # --- Tier 2: GroqCloud (Ultra-fast LPU inference) ---
        ModelEndpoint(
            tier=2,
            provider=ProviderType.GROQ,
            model_name="llama-3.3-70b-versatile",
            api_key_env_var="GROQ_API_KEY",
            base_url="https://api.groq.com/openai/v1",
        ),
        ModelEndpoint(
            tier=2,
            provider=ProviderType.GROQ,
            model_name="llama-3.1-8b-instant",
            api_key_env_var="GROQ_API_KEY",
            base_url="https://api.groq.com/openai/v1",
        ),
        ModelEndpoint(
            tier=2,
            provider=ProviderType.GROQ,
            model_name="mixtral-8x7b-32768",
            api_key_env_var="GROQ_API_KEY",
            base_url="https://api.groq.com/openai/v1",
        ),

        # --- Tier 3: Cerebras Cloud (Wafer-scale speed) ---
        ModelEndpoint(
            tier=3,
            provider=ProviderType.CEREBRAS,
            model_name="llama3.1-8b",
            api_key_env_var="CEREBRAS_API_KEY",
            base_url="https://api.cerebras.ai/v1",
        ),
        ModelEndpoint(
            tier=3,
            provider=ProviderType.CEREBRAS,
            model_name="llama3.3-70b",
            api_key_env_var="CEREBRAS_API_KEY",
            base_url="https://api.cerebras.ai/v1",
        ),

        # --- Tier 4: OpenRouter (Universal Gateway with free community pool) ---
        ModelEndpoint(
            tier=4,
            provider=ProviderType.OPENROUTER,
            model_name="openrouter/free",
            api_key_env_var="OPENROUTER_API_KEY",
            base_url="https://openrouter.ai/api/v1",
        ),
        ModelEndpoint(
            tier=4,
            provider=ProviderType.OPENROUTER,
            model_name="qwen/qwen3.8-27b:free",
            api_key_env_var="OPENROUTER_API_KEY",
            base_url="https://openrouter.ai/api/v1",
        ),
        ModelEndpoint(
            tier=4,
            provider=ProviderType.OPENROUTER,
            model_name="nvidia/nemotron-3.5-lightning:free",
            api_key_env_var="OPENROUTER_API_KEY",
            base_url="https://openrouter.ai/api/v1",
        ),

        # --- Tier 5: Mistral AI ---
        ModelEndpoint(
            tier=5,
            provider=ProviderType.MISTRAL,
            model_name="mistral-small-latest",
            api_key_env_var="MISTRAL_API_KEY",
            base_url="https://api.mistral.ai/v1",
        ),
        ModelEndpoint(
            tier=5,
            provider=ProviderType.MISTRAL,
            model_name="open-mistral-7b",
            api_key_env_var="MISTRAL_API_KEY",
            base_url="https://api.mistral.ai/v1",
        ),

        # --- Tier 6: Cloudflare Workers AI ---
        ModelEndpoint(
            tier=6,
            provider=ProviderType.CLOUDFLARE,
            model_name="@cf/meta/llama-3.1-8b-instruct",
            api_key_env_var="CLOUDFLARE_API_KEY",
            account_id_env_var="CLOUDFLARE_ACCOUNT_ID",
        ),
    ]


# Cache clients across calls to avoid recreating SSL connection pools
_OPENAI_CLIENT_CACHE: Dict[str, Any] = {}


def _call_google_genai(endpoint: ModelEndpoint, prompt: str, system_prompt: Optional[str] = None) -> str:
    """
    Executes a prompt against Google AI Studio using the official SDK.
    Supports either google-genai (v1+) or google-generativeai.
    """
    api_key = endpoint.api_key
    if not api_key:
        raise ValueError(f"Missing environment variable {endpoint.api_key_env_var}")

    # Try modern google-genai SDK first
    try:
        from google import genai
        from google.genai import types

        client = genai.Client(api_key=api_key)
        config = types.GenerateContentConfig(
            temperature=0.2,
        )
        if system_prompt:
            config.system_instruction = system_prompt

        response = client.models.generate_content(
            model=endpoint.model_name,
            contents=prompt,
            config=config,
        )
        text = response.text
        if not text:
            raise ValueError("Empty response received from Google GenAI")
        return text.strip()

    except ImportError:
        # Fallback to google-generativeai SDK
        try:
            import google.generativeai as legacy_genai

            legacy_genai.configure(api_key=api_key)
            model = legacy_genai.GenerativeModel(
                model_name=endpoint.model_name,
                system_instruction=system_prompt if system_prompt else None,
            )
            response = model.generate_content(
                prompt,
                generation_config={"temperature": 0.2},
                request_options={"timeout": REQUEST_TIMEOUT},
            )
            text = response.text
            if not text:
                raise ValueError("Empty response received from Google GenerativeAI")
            return text.strip()

        except ImportError:
            # If neither SDK is installed, fall back to pure REST via requests/urllib
            import json
            import urllib.request
            import urllib.error

            url = f"https://generativelanguage.googleapis.com/v1beta/models/{endpoint.model_name}:generateContent?key={api_key}"
            payload: Dict[str, Any] = {
                "contents": [{"parts": [{"text": prompt}]}],
                "generationConfig": {"temperature": 0.2},
            }
            if system_prompt:
                payload["systemInstruction"] = {"parts": [{"text": system_prompt}]}

            req = urllib.request.Request(
                url,
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST",
            )
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
                data = json.loads(resp.read().decode("utf-8"))
                candidates = data.get("candidates", [])
                if candidates:
                    parts = candidates[0].get("content", {}).get("parts", [])
                    if parts and "text" in parts[0]:
                        return parts[0]["text"].strip()
                raise ValueError(f"Unexpected response structure: {data}")


def _call_openai_compatible(endpoint: ModelEndpoint, prompt: str, system_prompt: Optional[str] = None) -> str:
    """
    Executes a prompt against OpenAI-compatible APIs (Groq, Cerebras, OpenRouter, Mistral, Cloudflare)
    using the standard `openai` Python SDK.
    """
    api_key = endpoint.api_key
    if not api_key:
        raise ValueError(f"Missing environment variable {endpoint.api_key_env_var}")

    base_url = endpoint.get_effective_base_url()
    if not base_url:
        raise ValueError(
            f"Base URL unavailable for {endpoint.provider}. "
            f"Ensure {endpoint.account_id_env_var or 'base_url'} is configured."
        )

    # Use standard openai SDK
    try:
        from openai import OpenAI
    except ImportError:
        # Fallback to direct HTTP request if openai SDK is not installed in the environment
        import json
        import urllib.request
        import urllib.error

        headers = {
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        }
        if endpoint.provider == ProviderType.OPENROUTER:
            headers["HTTP-Referer"] = "https://github.com/soumyarana77789/samsung-ai-remote"
            headers["X-Title"] = "Samsung AI Remote Assistant"

        messages = []
        if system_prompt:
            messages.append({"role": "system", "content": system_prompt})
        messages.append({"role": "user", "content": prompt})

        payload = {
            "model": endpoint.model_name,
            "messages": messages,
            "temperature": 0.2,
        }

        full_url = f"{base_url.rstrip('/')}/chat/completions"
        req = urllib.request.Request(
            full_url,
            data=json.dumps(payload).encode("utf-8"),
            headers=headers,
            method="POST",
        )
        with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            choices = data.get("choices", [])
            if choices and "message" in choices[0]:
                content = choices[0]["message"].get("content", "")
                if content:
                    return content.strip()
            raise ValueError(f"Unexpected response payload: {data}")

    # Re-use or instantiate cached client
    cache_key = f"{endpoint.provider}:{base_url}:{api_key[:8]}"
    if cache_key not in _OPENAI_CLIENT_CACHE:
        default_headers = {}
        if endpoint.provider == ProviderType.OPENROUTER:
            default_headers["HTTP-Referer"] = "https://github.com/soumyarana77789/samsung-ai-remote"
            default_headers["X-Title"] = "Samsung AI Remote Assistant"

        _OPENAI_CLIENT_CACHE[cache_key] = OpenAI(
            api_key=api_key,
            base_url=base_url,
            timeout=REQUEST_TIMEOUT,
            default_headers=default_headers if default_headers else None,
        )

    client: OpenAI = _OPENAI_CLIENT_CACHE[cache_key]

    messages = []
    if system_prompt:
        messages.append({"role": "system", "content": system_prompt})
    messages.append({"role": "user", "content": prompt})

    response = client.chat.completions.create(
        model=endpoint.model_name,
        messages=messages,
        temperature=0.2,
        timeout=REQUEST_TIMEOUT,
    )

    if not response.choices or not response.choices[0].message.content:
        raise ValueError("Received empty choices/content from model endpoint.")

    return response.choices[0].message.content.strip()


def execute_endpoint(endpoint: ModelEndpoint, prompt: str, system_prompt: Optional[str] = None) -> str:
    """Dispatches the call to the appropriate engine handler based on provider type."""
    if endpoint.provider == ProviderType.GOOGLE:
        return _call_google_genai(endpoint, prompt, system_prompt)
    else:
        return _call_openai_compatible(endpoint, prompt, system_prompt)


def generate_completion_with_fallback(
    prompt: str,
    system_prompt: Optional[str] = None,
    chain: Optional[List[ModelEndpoint]] = None,
) -> str:
    """
    Main resilient wrapper function. Iterates strictly through the 13 fallback targets.

    If any model or provider encounters rate limits (429), timeouts, server errors (500/503),
    or auth issues, it catches the error, outputs a clean warning log, and immediately
    retries with the next target.

    Raises:
        RuntimeError: If ALL 13 target endpoints fail.
    """
    endpoints = chain or build_fallback_chain()
    total_endpoints = len(endpoints)

    for index, endpoint in enumerate(endpoints, start=1):
        # Quick validation: check if key is present before burning a network timeout
        if not endpoint.api_key:
            err_msg = f"Key '{endpoint.api_key_env_var}' is not set in environment"
            print(f"[WARN] Model {endpoint.model_name} on {endpoint.provider.value} failed ({err_msg}). Trying next target...", file=sys.stderr)
            continue

        if endpoint.provider == ProviderType.CLOUDFLARE and not endpoint.account_id:
            err_msg = f"Cloudflare Account ID '{endpoint.account_id_env_var}' is not set"
            print(f"[WARN] Model {endpoint.model_name} on {endpoint.provider.value} failed ({err_msg}). Trying next target...", file=sys.stderr)
            continue

        try:
            start_time = time.time()
            logger.info(f"[{index}/{total_endpoints}] Attempting {endpoint.model_name} on {endpoint.provider.value} (Tier {endpoint.tier})...")
            
            result = execute_endpoint(endpoint, prompt, system_prompt)
            
            elapsed = time.time() - start_time
            logger.info(f"✅ Success with {endpoint.model_name} ({endpoint.provider.value}) in {elapsed:.2f}s!")
            return result

        except Exception as e:
            # Clean single-line error description
            error_desc = str(e).strip()
            if len(error_desc) > 120:
                error_desc = error_desc[:117] + "..."
            
            # User requirement: log warning in exact requested format
            print(
                f"[WARN] Model {endpoint.model_name} on {endpoint.provider.value} failed ({error_desc}). Trying next target...",
                file=sys.stderr
            )

    # If all endpoints failed
    raise RuntimeError("All LLM providers failed.")


def sync_keys_from_local_server(server_url: str) -> bool:
    """
    Pulls API keys directly from the Android Phone's Local HTTP Server (/api/keys).
    This allows running the Python script on Termux or PC without typing any credentials,
    by retrieving them over local Wi-Fi from the Samsung Remote Android app.
    """
    import urllib.request
    import json

    clean_url = server_url.rstrip("/")
    if not clean_url.startswith("http://") and not clean_url.startswith("https://"):
        clean_url = f"http://{clean_url}"

    endpoint_url = f"{clean_url}/api/keys"
    logger.info(f"Connecting to Phone Local Server at {endpoint_url}...")

    try:
        req = urllib.request.Request(
            endpoint_url,
            headers={"User-Agent": "LLMFallbackEngine/1.0", "Accept": "application/json"},
        )
        with urllib.request.urlopen(req, timeout=5) as response:
            if response.status != 200:
                print(f"[WARN] Local server returned HTTP {response.status}", file=sys.stderr)
                return False
            payload = json.loads(response.read().decode("utf-8"))

        keys_dict = payload.get("keys", {})
        loaded_count = 0
        for env_var, key_val in keys_dict.items():
            if key_val and str(key_val).strip():
                os.environ[env_var] = str(key_val).strip()
                loaded_count += 1

        print(f"\n[SYNC] 📡 Successfully pulled {loaded_count} API keys from Phone Server ({clean_url})!")
        return True
    except Exception as e:
        print(f"[WARN] Could not sync keys from {endpoint_url}: {e}", file=sys.stderr)
        return False


def print_environment_status():
    """Prints a clear summary of which API keys are detected."""
    keys = [
        ("Tier 1", "Google AI Studio", "GEMINI_API_KEY"),
        ("Tier 2", "GroqCloud", "GROQ_API_KEY"),
        ("Tier 3", "Cerebras Cloud", "CEREBRAS_API_KEY"),
        ("Tier 4", "OpenRouter", "OPENROUTER_API_KEY"),
        ("Tier 5", "Mistral AI", "MISTRAL_API_KEY"),
        ("Tier 6", "Cloudflare AI", "CLOUDFLARE_API_KEY"),
        ("Tier 6", "Cloudflare Account ID", "CLOUDFLARE_ACCOUNT_ID"),
    ]
    print("\n" + "=" * 65)
    print("      MULTI-PROVIDER LLM FALLBACK SYSTEM: KEY STATUS")
    print("=" * 65)
    for tier, provider, env_var in keys:
        val = os.getenv(env_var, "").strip()
        status = f"🟢 DETECTED ({val[:4]}...{val[-4:]})" if len(val) >= 8 else ("🟡 SET (short)" if val else "⚪ NOT SET")
        print(f" {tier:<8} | {provider:<22} | {env_var:<22} | {status}")
    print("=" * 65 + "\n")


if __name__ == "__main__":
    # Check for --sync <url> argument or PHONE_SERVER_URL environment variable
    cli_args = list(sys.argv[1:])
    sync_url = os.getenv("PHONE_SERVER_URL")

    if "--sync" in cli_args:
        idx = cli_args.index("--sync")
        if idx + 1 < len(cli_args):
            sync_url = cli_args[idx + 1]
            cli_args.pop(idx + 1)
        cli_args.pop(idx)

    if sync_url:
        sync_keys_from_local_server(sync_url)

    print_environment_status()

    # If prompt passed via CLI argument:
    test_prompt = " ".join(cli_args) if cli_args else (
        "Explain in 2 sentences how a Samsung TV remote app communicates with Tizen OS over WebSocket."
    )

    print(f"Testing Fallback System with prompt:\n\"{test_prompt}\"\n")
    try:
        completion = generate_completion_with_fallback(
            prompt=test_prompt,
            system_prompt="You are an expert TV system engineer. Be concise, direct, and factual."
        )
        print("\n" + "-" * 40 + " FINAL COMPLETION " + "-" * 40)
        print(completion)
        print("-" * 98 + "\n")
    except RuntimeError as err:
        print(f"\n❌ FATAL: {err}", file=sys.stderr)
        sys.exit(1)
