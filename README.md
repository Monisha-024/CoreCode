# CoreCode

### AI Developer Knowledge & Policy Assistant

CoreCode is an AI-powered Java/Spring Boot application that combines **company policies**, **source code**, and **Git history** into a single AI assistant.

It answers developer questions using retrieved project evidence and provides source-cited responses instead of relying on unsupported assumptions.

## 1. Problem Statement

In a software company, senior developers accumulate years of tacit
knowledge: why code was written a certain way, why a workaround exists, what
happened the last time an implementation changed. When they leave, that
knowledge leaves with them. Meanwhile, companies maintain many policy
documents (security, coding standards, remote work, data handling) that
developers are expected to know but rarely read in full.

## 2. Proposed Solution

An AI assistant, backed by a relational database of policies, source code,
and commit history, that:

- Classifies each question (POLICY / CODE / GIT_HISTORY / COMBINED / GENERAL)
- Retrieves only the relevant, verified records from MySQL
- Sends that evidence — and only that evidence — to Gemini
- Returns an answer **with the sources it used**
- Explicitly refuses to invent policies, commits, or code behavior it has no
  evidence for

## 3. Key Features

- Admin policy management with **versioning** (only one ACTIVE version per
  policy; old versions are ARCHIVED automatically)
- Real PDF text extraction (Apache PDFBox) → chunked → stored in MySQL
- Simple keyword-overlap **RAG retrieval** (no vector DB required)
- Real GitHub REST API integration: file tree, Java source, commit history
- Code Explorer with **Explain Code** / **Why Was This Changed?** actions
- **Combined Policy + Code + Git** answers — the project's signature feature
- Every AI answer displays its evidence (policy section, file, or commit)
- Hallucination-prevention rules enforced in every Gemini prompt
- JWT authentication with Spring Security; roles enforced server-side only
- Sample data (3 policies, 2 demo accounts) seeded automatically on first run

## 4. Architecture

```
                         USER
                           |
                           v
                 HTML / CSS / Vanilla JS
                           |
                           v
                      REST APIs (JWT)
                           |
                           v
                  Spring Boot Backend (monolith)
                           |
          +----------------+----------------+
          |                |                |
          v                v                v
        MySQL          GitHub API       Gemini API
          |                |                |
          v                v                v
   Policies/Versions   Source Code      AI Reasoning
   Users/Queries       Commits          (grounded, cited)
```

The backend owns: authentication/authorization, policy versioning, PDF
extraction, code/commit retrieval, question classification, RAG context
building, Gemini communication, and source tracking.

## 5. Tech Stack

| Layer          | Technology                                   |
|----------------|-----------------------------------------------|
| Frontend       | HTML5, CSS3, Vanilla JavaScript                |
| Backend        | Java 17+, Spring Boot 3.3, Maven               |
| Database       | MySQL, Spring Data JPA, Hibernate              |
| Auth           | Spring Security, JWT (jjwt), BCrypt            |
| AI             | Google Gemini API                              |
| VCS Data       | GitHub REST API                                |
| PDF            | Apache PDFBox                                  |
| Testing        | JUnit 5, Spring Boot Test, Mockito             |

No React, Firebase, MongoDB, microservices, vector databases, or ML
training — intentionally, to keep the project scoped and explainable.

## 6. Database Schema

```
users(id, name, email, password_hash, role, created_at)
policies(id, name, description, current_version_id, created_at, updated_at)
policy_versions(id, policy_id, version_number, file_name, effective_date,
                 status, content, created_at)
policy_chunks(id, policy_version_id, chunk_index, content)
repositories(id, name, owner, github_url, default_branch, connected_by, created_at)
code_files(id, repository_id, file_path, language, content, last_updated)
commits(id, repository_id, commit_hash, message, author, commit_date, changed_files)
queries(id, user_id, question, question_type, answer, created_at)
query_sources(id, query_id, source_type, source_id, source_label)
```

See `backend/src/main/resources/schema.sql` for the full reference DDL
(indexes and foreign keys included). Hibernate (`ddl-auto=update`) creates
and maintains these tables automatically — the SQL file is for
documentation/manual setup only.

## 7. API Endpoints

```
POST   /api/auth/register
POST   /api/auth/login

POST   /api/policies                                    [ADMIN]
GET    /api/policies
GET    /api/policies/{id}
DELETE /api/policies/{id}                                [ADMIN]
POST   /api/policies/{id}/versions          (multipart)  [ADMIN]
GET    /api/policies/{id}/versions
PUT    /api/policies/{id}/versions/{v}/activate           [ADMIN]
PUT    /api/policies/{id}/versions/{v}/archive            [ADMIN]
GET    /api/policies/{id}/compare?from=&to=

POST   /api/repositories                                  [ADMIN]
GET    /api/repositories
GET    /api/repositories/{id}/files
GET    /api/repositories/{id}/files/content?path=
GET    /api/repositories/{id}/commits?path=

POST   /api/assistant/ask
```

## 8. Authentication

- Passwords hashed with BCrypt
- Login returns a JWT (24h expiry by default) containing the user's role
- The frontend stores the JWT in `localStorage` and sends
  `Authorization: Bearer <token>` on every request
- **The backend is the sole source of truth for authorization** — the role
  claimed by the frontend is never trusted; every protected endpoint checks
  the role embedded in the verified JWT via Spring Security

## 9. Setup

### 9.1 Prerequisites

- Java 17+
- Maven 3.9+
- MySQL 8+
- A Gemini API key: https://aistudio.google.com/app/apikey
- A GitHub Personal Access Token (read-only `repo` scope is enough):
  https://github.com/settings/tokens

### 9.2 MySQL Setup

```sql
CREATE DATABASE devassistant;
```

That's it — Hibernate creates all tables on first run. (The full schema is
also in `backend/src/main/resources/schema.sql` if you prefer to create it
manually.)

### 9.3 Environment Variables

Copy `.env.example` to `.env` and fill in real values, or export the same
variables in your shell:

```
DB_URL=jdbc:mysql://localhost:3306/devassistant?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
JWT_SECRET=a-long-random-string-at-least-32-characters
GEMINI_API_KEY=your_gemini_api_key
GITHUB_TOKEN=your_github_personal_access_token
```

Spring Boot reads environment variables directly (see
`application.properties`), so however you load `.env` into your shell
(e.g. `export $(cat .env | xargs)`, or an IDE run-configuration) works.

**If `GEMINI_API_KEY` or `GITHUB_TOKEN` is missing**, the relevant feature
fails gracefully with a clear `502` error message (`"[Gemini] GEMINI_API_KEY
is not configured..."`) instead of silently returning fake data.

### 9.4 Running the Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. On first run, `DataSeeder`
creates two demo accounts and three sample policies automatically.

### 9.5 Running the Frontend

The frontend is plain static HTML/CSS/JS — no build step. Serve it with any
static file server, for example:

```bash
cd frontend
python3 -m http.server 5500
```

Then open `http://localhost:5500/login.html`. (The default CORS
configuration already allows `http://localhost:5500`.)

### 9.6 Testing

```bash
cd backend
mvn test
```

Covers: `QuestionClassifierServiceTest`, `PolicyComparisonServiceTest`,
`PolicyRetrievalServiceTest`, `PdfExtractionServiceTest`,
`PolicyVersionServiceTest` (activation/archiving rules, duplicate-name
rejection).

## 10. Sample Login Accounts

| Role      | Email                       | Password    |
|-----------|------------------------------|-------------|
| Admin     | admin@devassistant.com       | Admin@123   |
| Developer | developer@devassistant.com   | Dev@12345   |

Seeded automatically — no manual setup required.

## 11. Sample Demo Questions

**Policy**
- "What is the current password requirement?"
- "Can I upload company code to my personal GitHub?"
- "What is the current MFA requirement?"

**Historical policy**
- "What was the password requirement in V1?"
- "What changed between V1 and V2?" *(use the Compare Versions panel on the
  policy details page, or ask the assistant directly)*

**Code** *(after connecting a repository and selecting a file)*
- "What does this method do?"
- "Explain this code."

**Git history**
- "Why was this file changed?"
- "What was changed in the most recent commit?"

**Combined** *(the signature feature)*
- "Can I modify this authentication function according to the current
  security policy?"

## 12. Architecture & CS Concepts Demonstrated

- **Layered architecture**: controller → service → repository, with DTOs
  isolating the API contract from JPA entities
- **Relational database design**: normalized schema, foreign keys, indexes,
  a policy/version/chunk hierarchy modeling "current vs. historical" state
- **Authentication & authorization**: JWT-based stateless auth, BCrypt
  hashing, method/route-level role enforcement that never trusts the client
- **RAG (Retrieval-Augmented Generation)**: a transparent, swappable
  keyword-overlap retrieval step (`PolicyRetrievalService`) isolated behind
  an interface-like service boundary so it could later be replaced with
  embeddings without touching callers
- **Rule-based classification**: a deterministic, explainable classifier
  (`QuestionClassifierService`) instead of a black-box ML model
- **External API integration**: real GitHub REST API and Gemini API calls
  with graceful degradation when credentials are missing
- **Prompt engineering for grounding**: a single shared system instruction
  (`GeminiService`) that forbids fabrication and requires source attribution
- **Text processing**: PDF extraction and chunking (Apache PDFBox), and a
  simple line-diff algorithm for policy version comparison
- **Testing**: JUnit 5 + Mockito unit tests around the business-critical
  rules (versioning invariants, classification, retrieval ranking)

## 13. Project Structure

```
developer-knowledge-assistant/
├── backend/
│   ├── src/main/java/com/example/devassistant/
│   │   ├── controller/   REST endpoints
│   │   ├── service/      Business logic (RAG pipeline, PDF, GitHub, Gemini)
│   │   ├── repository/   Spring Data JPA interfaces
│   │   ├── model/        JPA entities + enums
│   │   ├── dto/          Request/response objects
│   │   ├── security/     JWT filter/util, UserDetailsService
│   │   ├── config/       Security config, CORS, DataSeeder
│   │   └── exception/    Custom exceptions + global handler
│   ├── src/test/java/... service-layer JUnit tests
│   ├── src/main/resources/
│   │   ├── application.properties
│   │   └── schema.sql   (reference only; Hibernate manages the schema)
│   └── pom.xml
├── frontend/
│   ├── login.html / register.html
│   ├── dashboard.html
│   ├── policies.html / policy-details.html
│   ├── repositories.html / code-explorer.html
│   ├── assistant.html
│   ├── css/styles.css
│   └── js/api.js, common.js
├── uploads/policies/     (local file storage placeholder)
├── .env.example
├── .gitignore
└── README.md
```

## 14. Future Enhancements

- Swap keyword-overlap retrieval for embeddings-based semantic search
- Support non-Java languages in the Code Explorer
- Webhook-based incremental repository sync instead of full re-sync
- Per-policy access control (some policies visible only to certain teams)
- Streaming Gemini responses in the chat UI
