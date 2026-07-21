# JobGraph

An automated job-application assistant that connects to your Gmail, extracts
job-opening emails, filters out spam/irrelevant alerts using an AI pipeline,
and builds a structured candidate profile from your resume — all with a
human-in-the-loop review step before anything gets acted on.

Built to demonstrate: Spring Boot backend engineering, a Python/LangGraph
agentic AI service, and Gmail API integration, working together as
independent microservices. A React frontend is in progress but not yet
pushed to this repo.

## Why this exists

Job-hunting means checking multiple platforms and inboxes for openings,
matching your resume to each one, and manually applying. JobGraph automates
the tedious middle steps — fetching, filtering, and profile-building — while
keeping a human in the loop for anything that matters (resume edits, final
submission). See [`/areas/jobgraph.md`](#) for the full design rationale.

## Architecture

```
┌──────────────────────┐     ┌────────────────────────┐
│  Spring Boot Backend │────▶│  Python AI Service     │
│  (jobgraph-backend)  │     │  (jobgraph-ai-service) │
│  Port 8080           │◀────│  Port 8000             │
└──────────┬───────────┘     └────────────────────────┘
            │
 ┌──────────▼───────────┐
 │  PostgreSQL + pgvector│
 └───────────────────────┘
```

- **Spring Boot** — core APIs, database, Gmail integration (OAuth + fetch +
  scheduled polling), resume text extraction.
- **Python (FastAPI + LangGraph)** — resume structuring via Gemini, and a
  LangGraph pipeline that classifies job-alert emails as spam or genuine,
  verifies real postings via web search, and ranks them by relevance.
- **PostgreSQL** — relational data (users, experience, projects, bullets,
  job status) with `pgvector` for future embedding-based job matching.

## Features implemented so far

### Profile creation
- Upload a resume (PDF) → text + hyperlinks extracted (Apache Tika + PDFBox,
  with position-based link-to-label matching so `GitHub Profile` stays
  attached to its actual URL instead of being separated).
- Extracted text sent to Gemini, which returns a structured JSON profile
  (skills, experience, projects, bullets with tags) grounded strictly in the
  source resume — no invented facts.
- Confirmed profile saved across four related tables: `users`, `experience`,
  `projects`, `bullets`.

### Gmail integration
- OAuth2 (read-only scope) via Google Cloud, token cached locally after
  first approval.
- Manual fetch endpoint and an automatic scheduled poll (every 10 minutes)
  that searches Gmail for job-related keywords/senders in the last 10 days.
- Fetched emails held in an in-memory stack (`ArrayDeque`), most recent on
  top.

### AI-based email filtering (LangGraph)
- **Classify relevance** — Gemini reads each email and judges whether it's a
  genuine job opening or spam/promo content, not just keyword matching.
- **Search verify** — extracts the actual "view this job" link from the
  email body, fetches that page, and asks Gemini whether it describes a
  real, currently open posting. Falls back to a general company search if
  no usable link is found.
- **Score & rank** — combines both signals into one relevance score;
  returns only surviving emails, ranked.

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 4, Java 21+, Maven |
| Database | PostgreSQL 18 + pgvector |
| AI service | Python 3.10+, FastAPI, LangGraph, Gemini API |
| Email | Gmail API (OAuth2) |
| Resume parsing | Apache Tika, PDFBox |
| Search (verification) | DuckDuckGo Search |
| Frontend *(in progress, not yet in repo)* | React (Vite), Tailwind CSS |

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
│                                  EmailController, GmailPollingJob
├── jobgraph-ai-service/          Python/FastAPI AI service
│   ├── main.py                    /parse-resume, /filter-emails endpoints
│   ├── graph.py                   LangGraph pipeline for email relevance filtering
│   └── requirements.txt
└── README.md

(A jobgraph-frontend/ React app exists locally and is in progress,
 but hasn't been pushed to this repo yet.)
```

## Setup

### Prerequisites
- JDK 21+, Maven (or `mvnw`)
- Python 3.10+
- Node.js + npm
- PostgreSQL 18 with the `pgvector` extension
- A Google Cloud project with the Gmail API enabled + OAuth credentials
- A free Gemini API key ([aistudio.google.com](https://aistudio.google.com))

### 1. Database
```sql
CREATE DATABASE jobgraph;
CREATE EXTENSION IF NOT EXISTS vector;
```
Run `jobgraph-backend/src/main/resources/schema.sql` (or let Hibernate
auto-create tables via `ddl-auto=update` in `application.properties`).

### 2. Spring Boot backend
```bash
cd jobgraph-backend
# edit src/main/resources/application.properties with your DB credentials
# place your Gmail OAuth credentials.json at src/main/resources/credentials.json
mvn spring-boot:run
```
Runs on `http://localhost:8080`.

### 3. Python AI service
```bash
cd jobgraph-ai-service
python -m venv venv
venv\Scripts\activate        # Windows
pip install -r requirements.txt
# create a .env file with: GEMINI_API_KEY=your_key_here
uvicorn main:app --reload --port 8000
```
Runs on `http://localhost:8000`.

### 4. Frontend *(in progress — not yet pushed to this repo)*
```bash
cd jobgraph-frontend
npm install
npm run dev
```
Runs on `http://localhost:5173`.

## API reference (current)

| Method | Endpoint | Service | Description |
|---|---|---|---|
| POST | `/profile/upload` | Spring Boot | Upload resume + extract text |
| POST | `/profile/save` | Spring Boot | Save confirmed profile to DB |
| POST | `/emails/fetch` | Spring Boot | Manually trigger Gmail fetch |
| GET | `/emails/stack` | Spring Boot | View fetched emails, newest first |
| GET | `/emails/stack/latest` | Spring Boot | Peek at the most recent email |
| POST | `/parse-resume` | Python | Structure raw resume text into JSON |
| POST | `/filter-emails` | Python | Classify, verify, and rank emails |

## Known limitations / deliberate scope decisions

- **Email stack is in-memory** — resets on app restart. Persisting fetched
  emails into the `job_status` table (already in the schema) is the next
  step.
- **Gmail OAuth uses a local desktop flow** (browser popup), not a
  production web OAuth redirect — fine for a personal-use tool, would need
  rework for multi-user deployment.
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
