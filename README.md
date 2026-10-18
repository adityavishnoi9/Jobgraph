# JobGraph

An automated job-application assistant that connects to your Gmail, extracts
job-opening emails, filters out spam/irrelevant alerts using an AI pipeline,
and builds a structured candidate profile from your resume — all with a
human-in-the-loop review step before anything gets acted on.

Built to demonstrate Spring Boot backend engineering, a Python/LangGraph
AI service, OpenAI integration, Gmail API integration, and a React frontend.

## Why this exists

Job-hunting means checking multiple platforms and inboxes for openings,
matching your resume to each one, and manually applying. JobGraph automates
the tedious middle steps — fetching, filtering, and profile-building — while
keeping a human in the loop for anything that matters (resume edits, final
submission). See [`/areas/jobgraph.md`](#) for the full design rationale.

## Architecture

```
React frontend ──▶ Spring Boot backend ──▶ OpenAI Responses API
                         │
                         └──▶ PostgreSQL + pgvector

Python AI service ──▶ OpenAI Responses API
```

- **Spring Boot** — core APIs, database, Gmail integration (OAuth + fetch),
  resume text extraction.
- **Python (FastAPI + LangGraph)** — resume structuring via OpenAI, and a
  LangGraph pipeline that classifies job-alert emails as spam or genuine,
  verifies real postings via web search, and ranks them by relevance.
- **PostgreSQL** — relational data (users, experience, projects, bullets,
  job status) with `pgvector` for future embedding-based job matching.

## Features implemented so far

### Profile creation
- Sign up or log in with an email and password before accessing the workspace.
- Upload a resume (PDF) → text + hyperlinks extracted (Apache Tika + PDFBox,
  with position-based link-to-label matching so `GitHub Profile` stays
  attached to its actual URL instead of being separated).
- Extracted text sent directly from Spring Boot to OpenAI, which returns a structured JSON profile
  (skills, experience, projects, bullets with tags) grounded strictly in the
  source resume — no invented facts.
- Review and edit all parsed fields before saving. Saving opens a separate
  profile page with the saved details and Gmail connection controls. Each
  account owns one profile across `users`, `experience`, `projects`, and
  `bullets`; saved profiles can be edited again after login.

### Gmail integration
- After saving a profile, enter the Gmail address to connect, then complete
  Google's sign-in, verification, and consent screens. The connected address
  must match the address entered in JobGraph.
  The Gmail address may differ from the JobGraph login email. Each account has
  one replaceable connection; its refresh token is encrypted in PostgreSQL.
- Manual fetch endpoint that searches Gmail for job-related keywords/senders
  in the last 10 days. The dedicated Inbox page shows the fetched messages with
  sender, subject, date, preview, and expandable message text.
- Fetched emails held in an in-memory stack (`ArrayDeque`), most recent on
  top; repeated fetches refresh messages with the same Gmail ID.

### AI-based email filtering (LangGraph)
- **Classify relevance** — OpenAI reads each email and judges whether it's a
  genuine job opening or spam/promo content, not just keyword matching.
- **Search verify** — extracts the actual "view this job" link from the
  email body, fetches that page, and asks OpenAI whether it describes a
  real, currently open posting. Falls back to a general company search if
  no usable link is found.
- **Score & rank** — combines both signals into one relevance score;
  returns only surviving emails, ranked.

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 4, Java 21+, Maven |
| Database | PostgreSQL 18 + pgvector |
| AI service | Python 3.10–3.13, FastAPI, LangGraph, OpenAI Responses API |
| Email | Gmail API (OAuth2) |
| Resume parsing | Apache Tika, PDFBox |
| Search (verification) | DuckDuckGo Search |
| Frontend | React (Vite), CSS |

## Directory structure

```
JobGraph/
├── jobgraph-backend/            Spring Boot service
│   └── src/main/java/com/JobGraph/jobgraph_backend/
│       ├── entity/               JPA entities (User, Experience, Project, Bullet, JobStatus)
│       ├── DTO/                  Request/response DTOs
│       ├── repository/           Spring Data repositories
│       ├── service/               ResumeParsingService, ProfileService
│       ├── controller/            ProfileController
│       └── gmail/                GmailAuthService, GmailFetchService, EmailStack,
│                                  EmailController, GmailConnectionController
├── jobgraph-ai-service/          Python/FastAPI AI service
│   ├── main.py                    /parse-resume, /filter-emails endpoints
│   ├── graph.py                   LangGraph pipeline for email relevance filtering
│   └── requirements.txt
└── README.md

The React app lives in `jobgraph-frontend/`.
```

## Setup

### Prerequisites
- JDK 21+, Maven (or `mvnw`)
- Python 3.10–3.13
- Node.js + npm
- PostgreSQL 18 with the `pgvector` extension
- A Google Cloud project with the Gmail API enabled + OAuth credentials
- An OpenAI API key with API billing enabled ([API keys](https://platform.openai.com/api-keys))

### 1. Database
```sql
CREATE DATABASE jobgraph;
CREATE EXTENSION IF NOT EXISTS vector;
```
Flyway creates and migrates the tables when the backend starts. On the existing
local database, the migration removes the one pre-account test profile and its
related records. It stops if it finds more than one legacy profile.

### 2. Spring Boot backend
```bash
# Copy jobgraph-ai-service/envexample to jobgraph-ai-service/.env and add
# OPENAI_API_KEY=your_key_here (OPENAI_MODEL is optional).
cd jobgraph-backend
# edit src/main/resources/application.properties with your DB credentials
mvn spring-boot:run
```
Runs on `http://localhost:8080`.
The backend loads the ignored `jobgraph-ai-service/.env` when launched from
`jobgraph-backend/` or the repository root. An exported `OPENAI_API_KEY` also works.

For Gmail, create a **Web application** OAuth client in Google Cloud, enable the
Gmail API, and add `http://localhost:8080/api/gmail/callback` as an authorized
redirect URI. Put these values in the ignored `jobgraph-ai-service/.env` file
alongside `OPENAI_API_KEY`:

```properties
GOOGLE_CLIENT_ID=your_web_client_id
GOOGLE_CLIENT_SECRET=your_web_client_secret
GMAIL_TOKEN_ENCRYPTION_KEY=base64_encoded_32_byte_key
```

Generate the encryption key with `openssl rand -base64 32`. Keep it stable:
changing it makes existing Gmail connections unreadable. Add your Google
account as an OAuth test user if the consent screen is in testing mode. The
backend still starts without these values, but Gmail connection reports a
configuration error until they are set.

### 3. Python AI service
```bash
cd jobgraph-ai-service
python3.13 -m venv .venv  # Python 3.10–3.13
source .venv/bin/activate  # macOS/Linux; on Windows use .venv\Scripts\activate
pip install -r requirements.txt
# use the same jobgraph-ai-service/.env file with OPENAI_API_KEY=your_key_here
uvicorn main:app --reload --port 8000
```
Runs on `http://localhost:8000`.

### 4. Frontend
```bash
cd jobgraph-frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

## API reference (current)

| Method | Endpoint | Service | Description |
|---|---|---|---|
| GET | `/api/auth/csrf` | Spring Boot | Get the CSRF token for writes |
| POST | `/api/auth/signup`, `/api/auth/login`, `/api/auth/logout` | Spring Boot | Session account access |
| GET | `/api/auth/me` | Spring Boot | Current account |
| POST | `/api/profile/upload` | Spring Boot | Parse a resume into an unsaved draft |
| GET, PUT | `/api/profile` | Spring Boot | Read or save the signed-in account's profile |
| GET | `/api/gmail/status`, `/api/gmail/callback` | Spring Boot | Gmail connection status and OAuth callback |
| POST | `/api/gmail/connect` | Spring Boot | Start Gmail connection for the entered address |
| DELETE | `/api/gmail/connection` | Spring Boot | Disconnect Gmail |
| POST | `/api/emails/fetch` | Spring Boot | Fetch from the signed-in account's Gmail |
| GET | `/api/emails/stack`, `/api/emails/stack/latest` | Spring Boot | View that account's fetched emails |
| POST | `/parse-resume` | Python | Structure raw resume text into JSON |
| POST | `/filter-emails` | Python | Classify, verify, and rank emails |

## Known limitations / deliberate scope decisions

- **Email stack is in-memory** — resets on app restart. Persisting fetched
  emails into the `job_status` table (already in the schema) is the next
  step.
- **Gmail OAuth needs Google web credentials** — automated tests mock Google;
  live consent requires the OAuth client settings above.
- **Form auto-fill on company portals is intentionally not fully
  autonomous** — the system prepares and pre-fills what it can; final
  submission stays a manual, human step by design.
- **Resume tailoring** (editing bullets to match a specific JD) is not yet
  built — planned as a constrained, grounded LangGraph step (select from a
  pre-approved bullet bank only, never fabricate).

## Roadmap

- [ ] Push React frontend to this repo (currently local-only, in progress)
- [ ] Persist emails into `job_status` table (replace in-memory-only stack)
- [ ] Matching engine: embed resume bullets + JD, compute relevance score
- [ ] LangGraph resume-tailoring graph with diff-based human review
- [ ] Frontend: profile review/edit screen, application dashboard
- [ ] Playwright-based partial form pre-fill for company portals
- [ ] Deploy to a free-tier host (Render/Railway) for 24/7 polling
