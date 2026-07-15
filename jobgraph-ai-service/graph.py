import json
import re
from typing import TypedDict, Optional

from langgraph.graph import StateGraph, END
from duckduckgo_search import DDGS
import google.generativeai as genai
import requests
from bs4 import BeautifulSoup

model = genai.GenerativeModel("gemini-3.5-flash")

# Links that are never the actual job posting - skip these when scanning
# an email body for the real "view this job" URL.
LINK_BLOCKLIST_KEYWORDS = [
    "unsubscribe", "mailto:", "privacy", "terms", "preferences",
    "tracking", "pixel", ".png", ".jpg", ".gif", "utm_source"
]

# Keywords that suggest a link IS the actual job posting, checked first.
JOB_LINK_HINT_KEYWORDS = [
    "job", "career", "position", "apply", "opening", "vacancy", "role"
]


class EmailState(TypedDict):
    subject: str
    sender: str
    body: str
    is_spam: bool
    confidence: float          # 0-1, how confident the LLM is this is a real job email
    company_name: Optional[str]
    legitimacy_score: float    # 0-1, how legitimate the verification step found this to be
    final_score: float
    reason: str


def _call_gemini_json(prompt: str) -> dict:
    """Calls Gemini and parses the response as JSON, stripping code fences if present."""
    response = model.generate_content(prompt)
    text = response.text.strip()
    if text.startswith("```"):
        text = text.strip("`")
        if text.startswith("json"):
            text = text[4:].strip()
    return json.loads(text)


def _extract_job_url(body: str) -> Optional[str]:
    """
    Finds the most likely "view this job" link inside the email body.
    Prefers links whose URL text contains job-related keywords; falls
    back to the first non-blocklisted link if nothing matches specifically.
    """
    urls = re.findall(r'https?://[^\s"<>\]]+', body)
    if not urls:
        return None

    candidates = [u for u in urls if not any(bad in u.lower() for bad in LINK_BLOCKLIST_KEYWORDS)]
    if not candidates:
        return None

    for url in candidates:
        if any(hint in url.lower() for hint in JOB_LINK_HINT_KEYWORDS):
            return url

    return candidates[0]


def _fetch_page_text(url: str, timeout: int = 6) -> Optional[str]:
    """Fetches a URL and returns its visible text content, or None on failure."""
    try:
        headers = {"User-Agent": "Mozilla/5.0 (JobGraph resume-automation project)"}
        response = requests.get(url, headers=headers, timeout=timeout, allow_redirects=True)
        response.raise_for_status()
        soup = BeautifulSoup(response.text, "html.parser")
        for tag in soup(["script", "style", "nav", "footer"]):
            tag.decompose()
        text = soup.get_text(separator=" ", strip=True)
        return text[:3000] if text else None
    except Exception:
        return None


# ---------------------------------------------------------------------------
# Node 1: classify relevance - is this actually a job opening, or spam/noise?
# ---------------------------------------------------------------------------
def classify_relevance(state: EmailState) -> EmailState:
    prompt = f"""You are filtering emails to find genuine job opening alerts.

Subject: {state['subject']}
From: {state['sender']}
Body (truncated): {state['body'][:1500]}

Return ONLY JSON, no markdown:
{{
  "is_spam": boolean,           // true if this is NOT a genuine job opening (promo, newsletter, course ad, unrelated notification, etc.)
  "confidence": number,         // 0.0 to 1.0 - how confident you are in this classification
  "company_name": string or null, // the hiring company's name, if this looks like a job email
  "reason": string              // one short sentence explaining the decision
}}
"""
    result = _call_gemini_json(prompt)
    state["is_spam"] = result.get("is_spam", True)
    state["confidence"] = float(result.get("confidence", 0.0))
    state["company_name"] = result.get("company_name")
    state["reason"] = result.get("reason", "")
    return state


def route_after_classification(state: EmailState) -> str:
    return "discard" if state["is_spam"] else "search_verify"


# ---------------------------------------------------------------------------
# Node 2: search verify - check the actual job posting link is real, or
# fall back to a general company search if no usable link was found.
# ---------------------------------------------------------------------------
def search_verify(state: EmailState) -> EmailState:
    job_url = _extract_job_url(state["body"])

    # Path A (preferred): the email contains an actual job link - fetch
    # that exact page and check it really describes an open role. This is
    # far more precise than a generic "{company} careers" search, which
    # for large companies returns their whole careers homepage rather
    # than confirmation of this specific opening.
    if job_url:
        page_text = _fetch_page_text(job_url)
        if page_text:
            prompt = f"""An email claims this is a real job opening. Here is the
actual content of the job link from that email:

URL: {job_url}
Page content: {page_text}

Return ONLY JSON, no markdown:
{{
  "legitimacy_score": number  // 0.0 to 1.0 - how much this page confirms a real, specific, currently open job posting (not an expired listing, generic homepage, or error page)
}}
"""
            try:
                result = _call_gemini_json(prompt)
                state["legitimacy_score"] = float(result.get("legitimacy_score", 0.3))
                return state
            except Exception:
                pass  # fall through to Path B if Gemini call fails

    # Path B (fallback): no usable job link found, or fetching it failed
    # (dead link, blocked by bot protection, etc.) - fall back to a
    # general company search as a weaker signal.
    company = state.get("company_name")
    if not company:
        state["legitimacy_score"] = 0.3
        return state

    try:
        with DDGS() as ddgs:
            search_results = list(ddgs.text(f"{company} careers hiring", max_results=3))
    except Exception:
        search_results = []

    snippets = "\n".join(r.get("body", "") for r in search_results) or "No results found."

    prompt = f"""A job alert email claims to be from the company "{company}",
but no direct job link could be verified. Here are general web search
results for "{company} careers hiring":

{snippets}

Return ONLY JSON, no markdown:
{{
  "legitimacy_score": number  // 0.0 to 1.0 - how much these results support "{company}" being a real, actively hiring company. Score this a bit more cautiously since it's a general check, not a direct posting check.
}}
"""
    try:
        result = _call_gemini_json(prompt)
        state["legitimacy_score"] = float(result.get("legitimacy_score", 0.3))
    except Exception:
        state["legitimacy_score"] = 0.3

    return state


# ---------------------------------------------------------------------------
# Node 3: score & rank - combine both signals into one final score
# ---------------------------------------------------------------------------
def score_and_rank(state: EmailState) -> EmailState:
    # Weighted average: classification confidence matters slightly more
    # than search legitimacy, since search results can be noisy/incomplete.
    state["final_score"] = round(0.6 * state["confidence"] + 0.4 * state["legitimacy_score"], 3)
    return state


# ---------------------------------------------------------------------------
# Build the graph
# ---------------------------------------------------------------------------
def build_graph():
    graph = StateGraph(EmailState)

    graph.add_node("classify_relevance", classify_relevance)
    graph.add_node("search_verify", search_verify)
    graph.add_node("score_and_rank", score_and_rank)

    graph.set_entry_point("classify_relevance")

    graph.add_conditional_edges(
        "classify_relevance",
        route_after_classification,
        {
            "discard": END,
            "search_verify": "search_verify",
        },
    )

    graph.add_edge("search_verify", "score_and_rank")
    graph.add_edge("score_and_rank", END)

    return graph.compile()


_compiled_graph = build_graph()


def filter_and_rank_emails(emails: list[dict]) -> list[dict]:
    """
    Runs each email through the graph individually (LangGraph state is
    per-email here, not batched) and returns only the surviving,
    non-spam emails sorted by final_score, highest first.
    """
    results = []

    for email in emails:
        initial_state: EmailState = {
            "subject": email.get("subject", ""),
            "sender": email.get("from", ""),
            "body": email.get("body", "") or email.get("snippet", ""),
            "is_spam": False,
            "confidence": 0.0,
            "company_name": None,
            "legitimacy_score": 0.0,
            "final_score": 0.0,
            "reason": "",
        }

        final_state = _compiled_graph.invoke(initial_state)

        # Discarded emails end at classify_relevance with is_spam=True and
        # never reach score_and_rank, so final_score stays 0 - skip those.
        if final_state.get("is_spam"):
            continue

        results.append({
            "messageId": email.get("messageId"),
            "subject": email.get("subject"),
            "from": email.get("from"),
            "companyName": final_state.get("company_name"),
            "confidence": final_state.get("confidence"),
            "legitimacyScore": final_state.get("legitimacy_score"),
            "finalScore": final_state.get("final_score"),
            "reason": final_state.get("reason"),
        })

    results.sort(key=lambda r: r["finalScore"], reverse=True)
    return results