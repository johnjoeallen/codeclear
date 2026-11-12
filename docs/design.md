# CodeClear — Design Specification

**Status:** Draft  
**Summary:** AI-powered code review tool that runs locally before a commit or push. Supports GitLab MRs, GitHub PRs, and local branch analysis with no MR required.

---

## Table of Contents

1. [Goals and Non-Goals](#1-goals-and-non-goals)
2. [High-Level Architecture](#2-high-level-architecture)
3. [Domain Model](#3-domain-model)
4. [Backend Design](#4-backend-design)
5. [Frontend Design](#5-frontend-design)
6. [REST API](#6-rest-api)
7. [AI Review Engine](#7-ai-review-engine)
8. [Git Platform Abstraction](#8-git-platform-abstraction)
9. [Local Branch Analysis](#9-local-branch-analysis)
10. [Storage and State](#10-storage-and-state)
11. [Configuration](#11-configuration)
12. [Open Questions](#12-open-questions)

---

## 1. Goals and Non-Goals

### Goals

- **Review anything reviewable**: an open MR on GitLab, an open PR on GitHub, or a local branch that has not yet been pushed.
- **No mandatory platform connection**: local-branch mode requires only a local git repo and AI credentials.
- **Minimal frontend footprint**: plain HTML + vanilla JS served as static files from the backend. No npm, no transpilation, no bundler.
- **Modern Java baseline**: Java 25 structured concurrency, virtual threads, string templates, and unnamed classes where appropriate. Spring Boot 4.x for the framework.
- **AI provider agnostic**: the AI tier is abstracted so GitHub Copilot, Claude, or OpenAI-compatible APIs can be swapped without touching the review pipeline.
- **No external issue tracker**: requirements come from the MR/PR description, local commit messages, or free-text provided at review time.

### Non-Goals

- Multi-user / multi-tenant deployment (single-user, localhost-only)
- GitHub PR approval workflows (requires app token; out of scope)
- CI/CD pipeline integration (deferred)

---

## 2. High-Level Architecture

The application runs entirely on the user's machine as a single self-contained JAR. A browser connects to a locally running Spring Boot 4 app on port 8080. The backend serves both a REST API (under `/api/`) and static frontend files from an embedded resources directory.

The review pipeline is composed of three stages: source resolution, AI review, and publishing. Source resolution has three implementations — local git (JGit), GitLab, and GitHub — all producing the same internal representation. The AI reviewer and publisher are both abstracted behind interfaces so providers can be swapped independently.

### 2.1 Launching and Platform

The app runs **natively on Windows** (Linux/macOS are not a first target) and is started from a launcher script rather than an installer:

- `codeclear.ps1` (PowerShell) and `codeclear.sh` (bash, e.g. Git Bash) are thin equivalents that do the same things: check prerequisites (Java 25, and `copilot.exe` when the Copilot provider is enabled), start the **prebuilt** JAR (`codeclear.jar`, distributed with the launcher scripts; users do not build from source), start the backend bound to `127.0.0.1`, and open the browser.
- The backend invokes the Windows `copilot.exe` directly. It is not run inside WSL, which avoids Windows/Linux path translation.
- The scripts contain bootstrap logic only; all behavior lives in the backend so the two scripts cannot drift apart.

Two filesystem locations are used for persistent state:

- `~/.codeClear/` — machine-level credentials and preferences; never committed.
- `{repo}/.codeClear/` — review history and the issue event store; committed alongside the code.

---

## 3. Domain Model

### 3.1 Review Source

A CC review source describes where the code to be reviewed comes from. There are two variants:

- **CCRemoteSource** — a GitLab MR or GitHub PR, identified by platform, URL, host, project reference, and number. Both platforms use the same type with a GITLAB/GITHUB discriminator.
- **CCLocalBranchSource** — a local git repository, identified by repo path, base branch, and diff scope (COMMITTED, STAGED, or ALL_CHANGES).

All sources resolve to the same internal representation — a list of CCChangedFile records with diffs and head content. The rest of the pipeline is source-agnostic.

### 3.2 Core Records

**CCChangedFile** — the unit of analysis. Carries the file path, previous path (renames only), change type (ADDED / MODIFIED / DELETED / RENAMED), the unified diff, the full file content at HEAD (omitted if too large), and a flag indicating whether content was skipped and why.

**CCFinding** — a single issue raised by the AI. Carries a unique ID, category, severity, confidence, file path, line range, enclosing method, title, evidence, impact, suggested remedy, whether the finding can be anchored to a diff line, a deduplication fingerprint, and related code references.

**CCAiAssessment** — the AI's overall verdict. Carries the verdict text, merge confidence score, deviation assessment (only when intent was provided), an AI-generated change description, the model used, instruction sources, and duration.

**CCReview** — the top-level aggregate. Carries review ID, status, source, resolved context, changed files, findings, reviewer selections, assessment, intent, and timestamps.

### 3.3 Review Context

CCReviewContext holds human-readable metadata regardless of source: title (MR/PR title or a synthetic "branch → base" label), source and target branch names, author username (absent for local), platform URL (absent for local), and addition/deletion/file counts.

### 3.4 Review Intent

Intent comes from one of two sources:

- **MR_SOURCE_OF_TRUTH** — the MR or PR description is used. For local branch reviews, the commit subject lines between base and HEAD are concatenated.
- **MANUAL_DESCRIPTION** — free text entered by the reviewer at review time.

CCReviewIntent carries the source enum and the resolved description text.

---

## 4. Backend Design

### 4.1 Technology Baseline

| Concern | Choice | Notes |
|---|---|---|
| Language | Java 25 | String templates, unnamed classes, structured concurrency GA |
| Framework | Spring Boot 4.x | Requires Java 21+; ships with Spring Framework 7 |
| Build | Maven 3.9+ | |
| HTTP client | Spring 6 RestClient | Fluent, synchronous; replaces RestTemplate |
| Git operations | JGit 7.x | Local repo access without shelling out |
| Diff parsing | java-diff-utils 4.x | |
| JSON | Jackson 2.x | Via Spring Boot autoconfigure |
| Issue store | Append-only Markdown files | Under `{repo}/.codeClear/issues/` |
| YAML parsing | SnakeYAML | Frontmatter parsing; Spring Boot transitive dependency |
| Server | Embedded Tomcat or Undertow | Virtual threads via Spring Boot 4 executor |

Virtual threads are enabled by default in Spring Boot 4 on Java 21+. All blocking pipeline steps benefit automatically with no thread pool tuning required.

Structured concurrency (Java 25) is used to fan out parallel file-content fetches. One subtask is forked per file; the scope joins all subtasks and propagates any failure immediately.

### 4.2 Package Structure

| Package | Responsibility |
|---|---|
| api.connect | Controllers for platform connection and session management |
| api.review | Controllers for review lifecycle, finding selection, and publishing |
| api.config | Controller for AI configuration |
| session | Single active session held in memory |
| platform | CCPlatformClient interface, factory, GitLab and GitHub implementations |
| local | JGit wrapper, diff extractor, and source resolver for local-branch mode |
| review | Pipeline orchestrator, progress service, selection service, file review store |
| review.model | CCReview, CCReviewStatus, CCReviewContext, CCReviewIntent, and related types |
| analysis.diff | Unified diff parser and changed-line map |
| analysis.source | File content resolver |
| analysis.model | CCChangedFile, CCFinding, CCFindingCategory, and related types |
| ai | CCAiReviewer interface, prompt builder, assessment parser, finding validator, instruction loader, redactor |
| ai.copilot | GitHub Copilot reviewer implementation and `copilot.exe` process runner |
| publishing | CCPublisher interface, publishing service, post preview and result models |
| credentials | Local credential store and model |
| prefs | User preferences store |
| config | Spring configuration and properties binding |

### 4.3 Key Architectural Decisions

**Session is optional for local reviews.** The session is only required when the source is a remote MR or PR and when publishing findings back. Session state is checked lazily.

**CCPlatformClient is a single interface for all platforms.** A factory inspects the source type and returns the correct implementation (GitLab, GitHub, or no-op for local).

**CCPublisher abstracts posting vs. exporting.** For remote reviews, it posts inline comments to the platform. For local reviews, it writes a Markdown report to disk.

**Assigned MR tracking is not included.** GitLab and GitHub have different assignee/reviewer concepts; deferred for a future iteration.

---

## 5. Frontend Design

### 5.1 Principles

- No framework, no build step, no node_modules.
- Single HTML file per page with shared CSS and shared JS utilities.
- ES modules loaded via the browser's native module system.
- Plain JS; JSDoc types where documentation helps.
- Served from Spring Boot's embedded resources as static content.
- Standard anchor-tag navigation between pages; no client-side router.

### 5.2 Pages and Static Files

| Path | Purpose |
|---|---|
| index.html | Entry point — redirects to connect or new-review page |
| connect.html | Platform connection |
| review-new.html | Start a new review |
| review-progress.html | Live progress during a running review |
| review-findings.html | Findings list with selection controls |
| review-confidence.html | Confidence score breakdown |
| review-deviation.html | Goal deviation view |
| reviews-history.html | Saved review history |
| issues.html | Cross-review issue tracker |
| css/reset.css | Browser normalisation |
| css/base.css | Design tokens, typography, layout |
| css/components.css | Buttons, cards, badges, dialogs |
| js/api.js | Fetch wrapper with error handling |
| js/session.js | Session state backed by sessionStorage |
| js/shell.js | Shared header, nav, and theme toggle |
| js/review-progress.js | SSE progress listener |
| js/issues.js | Issue tracker (filter, status updates, event timeline) |
| js/* | One JS module per page |

### 5.3 API Communication

All API calls go through a shared fetch wrapper (`api.js`). It prefixes all paths with `/api`, sets JSON content-type, and throws a structured error on non-2xx responses. A 204 response returns null.

### 5.4 Progress Streaming

Review progress is streamed via Server-Sent Events. The frontend opens a native EventSource connection and updates the progress bar with each step name, percentage, and message. The connection is closed when a COMPLETED or FAILED event arrives.

### 5.5 Layout and Theme

Layout uses CSS Grid and Flexbox with no framework. A shared shell (header and nav) is injected into each page by a shared script at load time. Light and dark themes are implemented with CSS custom properties; theme state is persisted in localStorage.

---

## 6. REST API

All endpoints are prefixed with `/api/`. Static files are served from the root with no prefix. All bodies are JSON.

### 6.1 Connection

| Method | Path | Description |
|---|---|---|
| POST | /api/connect | Connect with a platform token (in-memory session) |
| POST | /api/connect/save | Save credentials locally |
| GET | /api/session | Current session info |
| POST | /api/disconnect | Clear the session |
| GET | /api/credentials | List saved credential profiles (no secrets) |
| DELETE | /api/credentials/:name | Delete a credential profile |

The connect request body specifies the platform (GITLAB or GITHUB), the host URL, and the access token.

### 6.2 Reviews

| Method | Path | Description |
|---|---|---|
| POST | /api/reviews | Create a new review |
| GET | /api/reviews | Recent reviews (default limit 5) |
| GET | /api/reviews/saved | All saved reviews with storage info |
| GET | /api/reviews/:id | Full review detail |
| POST | /api/reviews/:id/run | Start the AI review (async) |
| DELETE | /api/reviews/:id | Delete a review |
| DELETE | /api/reviews | Delete all reviews (requires confirmation param) |

The create-review request specifies the source type (REMOTE or LOCAL). For remote sources, the full MR or PR URL is provided and the platform is inferred from it. For local sources, the repo path, base branch, and diff scope are provided. Both accept the intent source and optional description.

### 6.3 Findings and Publishing

| Method | Path | Description |
|---|---|---|
| GET | /api/reviews/:id/progress | SSE stream of progress events |
| PUT | /api/reviews/:id/findings/:fp/selection | Select, deselect, or dismiss a finding |
| GET | /api/reviews/:id/findings/:fp/code | Code context around a finding |
| GET | /api/reviews/:id/post-preview | Preview what would be posted |
| POST | /api/reviews/:id/post | Post findings to the platform (remote only) |
| GET | /api/reviews/:id/post-summary | Existing summary note metadata |
| GET | /api/reviews/:id/posted-fingerprints | Already-posted fingerprints |
| GET | /api/reviews/:id/export | Download a Markdown report |

### 6.4 AI Configuration

| Method | Path | Description |
|---|---|---|
| GET | /api/config/ai | Provider readiness, active model, loaded instruction files |
| GET | /api/config/ai/tip | A random coding tip from the AI |
| PUT | /api/config/ai/model | Update the default model preference |

### 6.5 Issues

| Method | Path | Description |
|---|---|---|
| GET | /api/issues | List issues (filter by status, severity, category) |
| GET | /api/issues/:fingerprint | Issue detail and full event history |
| PUT | /api/issues/:fingerprint/status | Append a state-change event |
| GET | /api/issues/stats | Issue counts by status and severity |

### 6.6 Health

| Method | Path | Description |
|---|---|---|
| GET | /api/health | Standard health check |

---

## 7. AI Review Engine

### 7.1 Interface

CCAiReviewer is a single-method interface: accept a review request and a progress listener, return a review result. The request carries resolved context, intent, files (with diffs and content), the loaded instruction set, and an optional model override. When no model is specified, the provider default is used.

### 7.2 Prompt Structure

Three prompts are used: an initial file batch, continuation prompts for subsequent batches, and a final assessment.

The context block varies by source: remote reviews include the MR/PR title, branches, author, and platform URL; local reviews include the repo path and branch names only.

The intent block includes the MR/PR description (or concatenated commit subject lines for local) when using MR_SOURCE_OF_TRUTH, or the user-provided text when using MANUAL_DESCRIPTION.

The deviation assessment is only included in the final prompt when intent text is non-empty and distinct from the change description.

### 7.3 Instruction Files

CC looks for a repo-level agent instruction file at `.codeClear/agent.md` and additional instruction files under `.codeClear/instructions/`. If absent, it falls back to defaults bundled in the JAR.

### 7.4 AI Provider Abstraction

Three implementations of CCAiReviewer are planned:

- **CCCopilotAiReviewer** — GitHub Copilot via the native `copilot.exe` CLI (the only fully implemented provider at launch; see 7.5)
- **CCAnthropicAiReviewer** — Claude via HTTP API
- **CCOpenAiCompatibleAiReviewer** — any OpenAI-compatible endpoint

The active provider is selected by configuration. Provider-specific settings are kept in namespaced config sections.

### 7.5 Copilot Provider (`copilot.exe`)

Copilot is integrated by launching the installed `copilot.exe` rather than calling an HTTP API:

- The reviewer spawns `copilot.exe` with `ProcessBuilder` in non-interactive (single prompt) mode, using the repo being reviewed as the working directory. The exact flags and output mode must be confirmed against the installed version.
- Authentication is owned by the CLI. CodeClear never sees Copilot tokens; `/api/config/ai` reports whether the executable was found and is signed in, and the UI shows setup guidance if not.
- The CLI cannot enforce a JSON schema, so the prompt instructs the model to emit findings as JSON. The reviewer extracts and validates it through the existing assessment parser and finding validator, and retries once with a repair prompt on failure.
- The executable path is configurable (default: resolved from `PATH`). Each prompt has a timeout (`prompt-timeout-seconds`); stdout and stderr are captured, and cancelling a review destroys the whole process tree.
- Review runs must be read-only: no file-write or shell tool permissions are granted to the CLI, since it runs inside the user's repo.
- Token and cost accounting is not available for this provider. Progress streaming is coarse (per prompt batch).

---

## 8. Git Platform Abstraction

### 8.1 CCPlatformClient Interface

All methods receive a CCRemoteSource and operate against whichever platform it describes. Operations:

- **Validate token** — verify credentials and return the authenticated user identity.
- **Resolve context** — fetch MR/PR metadata and return a CCReviewContext.
- **Fetch changed files** — retrieve diffs for all changed files and return CCChangedFile records.
- **Fetch file content** — retrieve a single file at a given commit SHA.
- **Post inline comment** — post a line-anchored diff comment.
- **Post summary note** — post a top-level comment.
- **Update summary note** — edit an existing summary comment by note ID.
- **Fetch discussions** — retrieve existing comments (used to detect already-posted findings).

### 8.2 GitLab Implementation

Uses the Spring 6 RestClient. Pagination for discussions is handled transparently. Changed files are fetched via the MR diffs API and normalised into CCChangedFile records. Inline comments are mapped to GitLab's position model (base SHA, head SHA, old/new path, line type).

### 8.3 GitHub Implementation

The project reference is treated as `owner/repo` and the number is the PR number.

| Operation | GitHub endpoint |
|---|---|
| Validate token | GET /user |
| Resolve context | GET /repos/{owner/repo}/pulls/{number} |
| Fetch changed files | GET /repos/{owner/repo}/pulls/{number}/files |
| Fetch file content | GET /repos/{owner/repo}/contents/{path} at ref |
| Post inline comment | POST /repos/{owner/repo}/pulls/{number}/comments |
| Post summary note | POST /repos/{owner/repo}/issues/{number}/comments |
| Fetch discussions | GET /repos/{owner/repo}/pulls/{number}/comments |

GitHub requires a commit SHA and diff line offset for inline comments; GitLab requires base/head SHAs and old/new paths. CCInlineComment carries all fields needed for either platform.

### 8.4 CCPlatformClientFactory

Selects the correct implementation from the source type and platform discriminator. A GitLab remote source yields a GitLab client. A GitHub remote source yields a GitHub client. A local branch source yields a no-op client.

---

## 9. Local Branch Analysis

### 9.1 Overview

Local-branch mode analyses code not yet pushed to any remote. No platform token is required. The user provides a path to the local git repository and a base branch to diff against. The backend uses JGit to compute the diff, read file content from the working tree, and feed the result through the standard AI pipeline.

### 9.2 CCLocalGitClient

Two operations:

- **Resolve context** — reads the HEAD branch name and base branch; returns a CCReviewContext with a synthetic "branch → base" title. No platform URL or author is populated.
- **Fetch changed files** — produces unified diffs using JGit's diff formatter; reads file content from the working tree; respects `.gitignore`.

### 9.3 Diff Scope

| Scope | What is compared |
|---|---|
| COMMITTED | Base branch tip to HEAD (committed changes only) |
| STAGED | Index vs HEAD (staged but not yet committed) |
| ALL_CHANGES | Working tree vs base branch (committed + staged + unstaged) |

Default is COMMITTED.

### 9.4 Intent for Local Reviews

Without an MR/PR description, CC defaults to the commit subject lines between base and HEAD formatted as a bullet list. The user can override this with MANUAL_DESCRIPTION.

### 9.5 Export for Local Reviews

The export endpoint (GET /api/reviews/:id/export) is the output mechanism for local reviews. It generates a Markdown report with the review context, confidence score, selected findings, and AI-generated change description. The frontend shows a "Download Report" button rather than a "Post to platform" button.

---

## 10. Storage and State

### 10.1 In-Memory State

Three items are held in memory for the server lifetime:

- The active session — null when no platform is connected.
- Running review executions — keyed by review ID; holds progress and intermediate results.
- Progress event queues — keyed by review ID; feeds the SSE progress endpoint.

### 10.2 Disk Layout

```
~/.codeClear/
├── credentials.yml         Platform tokens and AI provider config
└── prefs.yml               User preferences

{repo}/.codeClear/
├── reviews/
│   └── {review-id}/
│       ├── summary.md
│       └── findings.md
└── issues/
    └── {CATEGORY-fingerprint8}/
        └── {timestamp}-{author}-{event}.md
```

`~/.codeClear/` holds machine-level config and must never be committed. `{repo}/.codeClear/` holds the team's shared history and must be committed with the repo.

**Credentials file**: a named profiles list (each with platform, host, and token) plus an AI block specifying the active provider and provider-specific settings (CLI path, API key, endpoint, model name). Copilot credentials are never stored here; they remain inside `copilot.exe`. On Windows, the file lives under the user profile and should be restricted to the current user.

### 10.3 Append-Only Issue Event Store

Issues are stored as one directory per fingerprint containing one file per event. No file is ever modified after creation. Current state is derived by replaying all files in chronological order.

This makes git merges conflict-free: two developers writing events for the same fingerprint produce differently-named files that auto-merge without conflicts.

**File naming**: `{yyyy-MM-dd}T{HH-mm-ss}Z-{author}-{event}.md` (colons replaced with hyphens for filesystem portability). Example: `2026-11-09T14-11-00Z-john-open.md`.

**Directory naming**: `{CATEGORY_PREFIX}-{first-8-chars-of-fingerprint}`. Example: `SEC-ab3f92c1`.

### 10.4 Event File Formats

Each event file has a YAML frontmatter block (structured metadata) followed by an optional Markdown body (narrative).

**Open** — written when a finding is first recorded for a fingerprint. Frontmatter: event type, fingerprint, category, severity, file, line range, enclosing method, confidence, review ID, branch, author, timestamp. Body: title heading, Evidence, Impact, and Remedy subsections. If a second developer opens the same fingerprint in a different review, their file is treated as a "seen again" record; the earliest open event by timestamp is canonical.

**Resolved** — appended when the issue is fixed. Frontmatter records previous and new state, review ID, author, timestamp. Body is optional and records what was done.

**Wont-fix** — appended when the team accepts the risk. Same structure as resolved; body records the rationale.

**Reopened** — appended when a resolved or wont-fix issue reappears. Frontmatter only; no body required.

**Dismissed** — appended when a reviewer manually dismisses a finding in the UI.

CC never modifies or deletes existing event files.

### 10.5 State Derivation

1. Sort all event files in the fingerprint directory by timestamp.
2. The earliest open event provides the canonical issue record (title, evidence, impact, remedy, severity, category, location).
3. Walk remaining events in order: each open increments the seen count; each resolved/wont-fix/reopened/dismissed updates the status.
4. Current status is the status after the last state-change event.

Results are cached in memory and invalidated when the issues directory changes.

### 10.6 Review Documents

**summary.md** — written on completion. Frontmatter records review ID, source type, branch, base, repo path, date, confidence score, band, model, file count, line counts, and duration. Body contains the AI-generated verdict.

**findings.md** — an index of all findings. Body is a table with one row per finding: fingerprint (linked to the issue directory), severity, file, line, title, and current status.

### 10.7 Issue Lifecycle on Review Completion

When findings are written after a review:

- No directory exists → create it and write a new open event.
- Directory exists, current status is RESOLVED or WONT_FIX → write a reopened event.
- Directory exists, current status is OPEN or DISMISSED → write a new open event (seen again).

When a user acts in the UI: dismissing writes a dismissed event; marking resolved or wont-fix writes the corresponding state-change event.

### 10.8 Data Access Layer

No database. CCIssueStore and CCReviewStore read and write the filesystem directly.

**CCIssueStore**: load a single issue (replay event directory), load all issues with optional filters, load full event history for one issue, append a new event.

**CCReviewStore**: save a review summary, save a findings index, list recent reviews, load one review summary.

YAML frontmatter is parsed with SnakeYAML. The Markdown body is read as a raw string — no Markdown parsing is needed on the read path.

### 10.9 Issues UI

The issues page surfaces the issue event store as a cross-review dashboard. Users can filter by status, severity, category, and file path; view the full event timeline for any issue; and manually change status (resolve, dismiss, wont-fix) which appends an event file via the API.

---

## 11. Configuration

CC is configured through the bundled application properties file. Values can be overridden with environment-specific files or environment variables using standard Spring Boot conventions.

| Setting | Default | Description |
|---|---|---|
| server.port | 8080 | Listening port |
| server.address | 127.0.0.1 | Bound to localhost only |
| virtual threads | enabled | Spring Boot 4 default on Java 21+ |
| max-findings | 30 | Cap on findings per review |
| max-batch-chars | 100,000 | Maximum characters per AI prompt batch |
| max-batches | 40 | Maximum batches per review |
| full-file-max-chars | 48,000 | File content threshold; larger files are omitted from the prompt |
| prompt-timeout-seconds | 300 | Per-prompt AI call timeout |
| copilot.cli-path | `copilot.exe` | Path to the Copilot CLI; resolved from `PATH` by default |
| default-scope | COMMITTED | Default diff scope for local-branch reviews |
| agent-file | .codeClear/agent.md | Repo-level agent instruction file |
| instructions-dir | .codeClear/instructions | Repo-level instruction files directory |
| classpath-fallback | codeClear/agent.md | JAR-bundled fallback instructions |
| user-dir | ~/.codeClear | Machine-level storage root (never committed) |
| repo-dir | .codeClear | Repo-relative storage root (committed with the repo) |

---

## 12. Open Questions

- `copilot.exe`: which non-interactive flags and output format does the installed version support, and can it be run strictly read-only?
- Which models does Copilot expose through the CLI, and can the UI let the user choose one?
- Where are prebuilt JARs published (GitHub Releases is the likely answer), and does the launcher check for updates?
