import os
import json
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import google.generativeai as genai
from dotenv import load_dotenv
from graph import filter_and_rank_emails

load_dotenv()

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise RuntimeError("GEMINI_API_KEY not set. Copy .env.example to .env and add your key.")

genai.configure(api_key=GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-3.5-flash")

app = FastAPI(title="JobGraph AI Service")


class ParseResumeRequest(BaseModel):
    resume_text: str
    additional_info: str | None = None


# This is the exact schema the LLM must return - matches your Postgres
# tables (users, experience, projects, bullets) so Spring Boot can save
# it directly once the user reviews/confirms it on the frontend.
SCHEMA_INSTRUCTIONS = """
Return ONLY valid JSON (no markdown, no code fences, no commentary) matching
exactly this structure:

{
  "fname": string,
  "lname": string,
  "email": string,
  "linkedin_url": string or null,
  "coding_handles": { "github": string or null, "leetcode": string or null, "codeforces": string or null, "codechef": string or null },
  "skills": { "language": [string], "framework": [string], "tool": [string], "database": [string], "concept": [string] },
  "experience": [
    {
      "company_name": string,
      "role": string,
      "start_date": "YYYY-MM-DD or null",
      "end_date": "YYYY-MM-DD or null (null if currently working there)",
      "bullets": [ { "text": string, "tags": [string] } ]
    }
  ],
  "projects": [
    {
      "name": string,
      "description": string or null,
      "bullets": [ { "text": string, "tags": [string] } ]
    }
  ]
}

Rules:
- Only use information present in the resume text or the additional info given. Never invent facts, companies, skills, or numbers not present in the source.
- "tags" for each bullet should be the specific skills/technologies that bullet demonstrates (used later for job-matching).
- If a date is not present or unclear, use null rather than guessing.
- The resume text includes a "---LINKS---" section with "label: url" pairs - use these to correctly fill linkedin_url and coding_handles.
- If additional info is provided, merge it in as extra skills/experience/projects where relevant, without duplicating what's already in the resume.
"""


@app.post("/parse-resume")
def parse_resume(request: ParseResumeRequest):
    prompt = f"""{SCHEMA_INSTRUCTIONS}

RESUME TEXT:
{request.resume_text}

ADDITIONAL INFO FROM USER:
{request.additional_info or "None"}
"""

    try:
        response = model.generate_content(prompt)
        raw_output = response.text.strip()

        # Gemini sometimes wraps JSON in ```json fences despite instructions - strip if present.
        if raw_output.startswith("```"):
            raw_output = raw_output.strip("`")
            if raw_output.startswith("json"):
                raw_output = raw_output[4:].strip()

        parsed_json = json.loads(raw_output)
        return parsed_json

    except json.JSONDecodeError:
        raise HTTPException(status_code=500, detail="LLM did not return valid JSON. Try again.")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to parse resume: {str(e)}")


@app.get("/health")
def health():
    return {"status": "ok"}


class FilterEmailsRequest(BaseModel):
    emails: list[dict]


@app.post("/filter-emails")
def filter_emails(request: FilterEmailsRequest):
    """
    Runs each email through the LangGraph pipeline: classify relevance ->
    discard if spam -> verify company via web search -> score and rank.
    Returns only the surviving emails, sorted by relevance (best first).
    """
    try:
        ranked = filter_and_rank_emails(request.emails)
        return {"count": len(ranked), "results": ranked}
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to filter emails: {str(e)}")