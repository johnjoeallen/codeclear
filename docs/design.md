# CodeClear — Initial Design (Draft)

> Rough first-pass design. Everything here is a proposal to be challenged.

## 1. Purpose

CodeClear is a UI-based AI code review tool that gets your branch clear
**before** it reaches a PR/MR. You point it at a branch (local, GitHub, or
GitLab), it reviews the changes with an LLM of your choice, and you triage the
findings in a visual interface, fixing issues before human reviewers see them.

### Goals

- A visual review UI: diff viewer with inline findings, filtering, and triage.
- Three change sources: **local branches**, **GitHub** (branches/PRs), **GitLab** (branches/MRs).
- Three AI providers: **GitHub Copilot**, **OpenAI**, **Anthropic Claude**; user selects per review.
- Findings that are specific, located (file:line), severity-ranked, and actionable.
- Optionally publish accepted findings back to a PR/MR as comments.

### Non-goals (for now)

- Replacing human review or approval workflows.
- Full-repo static analysis or linter replacement.
- Auto-committing fixes without explicit user confirmation.

## 2. Form Factor (assumption — to confirm)

A **local web app**: a small backend started with `codeclear` that serves a
browser UI on `localhost`. Chosen because it needs direct access to local git
repos and local credentials, while still being a UI. Can later be wrapped as a
desktop app (Tauri/Electron) without changing the core.

## 3. User Experience

1. **Pick a source**
   - *Local*: choose a repo folder, then a head branch and base branch.
   - *GitHub / GitLab*: connect an account, choose repo, then a branch or an open PR/MR.
2. **Pick a provider and model** (Copilot / OpenAI / Claude) and review profile
   (e.g. correctness, security, tests).
3. **Run review** — progress is streamed to the UI.
4. **Triage findings** in a diff view: each finding is shown inline at its line
   with severity, explanation, and optional suggested patch. Actions: accept,
   dismiss, mark false-positive, copy patch / apply to working tree (local only).
5. **Publish** (GitHub/GitLab sources): post accepted findings as inline
   comments plus a summary on the PR/MR.
6. **History**: past reviews per branch; re-run and see what's new/resolved.

## 4. Architecture

```
                ┌───────────────────────────┐
                │        Web UI (SPA)       │
                └─────────────┬─────────────┘
                              │ HTTP / SSE
                ┌─────────────▼─────────────┐
                │        Backend API        │
                │  reviews · settings · auth│
                └──┬──────────┬──────────┬──┘
                   │          │          │
        ┌──────────▼───┐ ┌────▼─────┐ ┌──▼───────────┐
        │ Source       │ │ Review   │ │ Provider     │
        │ adapters     │ │ engine   │ │ adapters     │
        │ local/GH/GL  │ │          │ │ Copilot/OAI/ │
        └──────────────┘ └────┬─────┘ │ Claude       │
                              │       └──────────────┘
                        ┌─────▼─────┐
                        │ Store     │
                        │ (SQLite)  │
                        └───────────┘
```

### 4.1 Source adapters
Common interface producing a normalized `ChangeSet` (files, hunks, commit
messages, base/head refs, optional PR/MR metadata) and supporting `publish(findings)`.

| Adapter | Reads from | Publish |
|---------|-----------|---------|
| Local | `git` CLI (merge-base diff vs. chosen base) | n/a (UI only; optional apply patch) |
| GitHub | REST/GraphQL API (PR diff or branch compare) | PR review comments |
| GitLab | REST API (MR diff or branch compare) | MR discussions |

Self-hosted GitLab / GitHub Enterprise supported via configurable base URL.

### 4.2 Review engine
- Splits the change set into reviewable units (per file, per hunk group) and
  enriches each with surrounding file context within a token budget.
- Runs focused passes (correctness, security, tests, maintainability), each
  returning structured findings: `file`, `line`, `severity`, `category`,
  `summary`, `explanation`, optional `suggested_patch`, `confidence`.
- Merges/de-duplicates findings; optional verification pass to cut false
  positives; stable fingerprints so reruns match prior findings.
- Skips ignored paths (lockfiles, vendored, generated) and secret-looking files.

### 4.3 Provider adapters
Common interface: `complete(messages, json_schema) -> structured result`, with
streaming, retries, rate-limit handling, and token/cost accounting.

| Provider | Auth | Notes |
|----------|------|-------|
| Anthropic Claude | API key | Native structured output / tool use. |
| OpenAI | API key | Structured outputs via JSON schema. |
| GitHub Copilot | GitHub OAuth / device flow | Requires a Copilot entitlement; API surface and terms need investigation (see open questions). |

### 4.4 Store
SQLite: reviews, findings + triage state, source/provider config, run history.
Credentials are **not** kept here (see §6).

## 5. Configuration

- Global settings via the UI (providers, accounts, defaults); stored in the user config dir.
- Optional per-repo `.codeclear.yml` for shareable settings:

```yaml
base: main
ignore: ["**/*.lock", "vendor/**"]
passes: [correctness, security, tests]
min_severity: medium
rules:
  - "All public API changes need a changelog entry."
```

## 6. Security & Privacy

- Code is sent to the selected provider; the UI should show this clearly before the first run per repo.
- Tokens/API keys live in the OS keychain (fallback: encrypted file), never in repo config.
- Backend binds to `127.0.0.1` only, with a per-session token to prevent other local/web origins calling it.
- Secret-pattern filtering before anything is sent to a provider.
- No telemetry by default.

## 7. Tech Choices (tentative)

| Area | Proposal | Notes |
|------|----------|-------|
| Frontend | TypeScript + React (Vite) | Diff rendering via an existing diff component library. |
| Backend | TypeScript (Node) or Go | Keep language shared with UI if Node; Go if single-binary matters. To decide. |
| Git | shell out to `git` | Faithful to user's setup. |
| Storage | SQLite | |
| Packaging | npm/npx or single binary; later Tauri desktop | |

## 8. Milestones

1. **MVP** — local source, Claude provider, run review, show findings in diff UI.
2. **Providers** — add OpenAI and GitHub Copilot behind the common interface.
3. **Remote sources** — GitHub and GitLab read support (branches, PRs/MRs).
4. **Publish** — post findings back to PRs/MRs; fingerprint-based dedupe on rerun.
5. **Quality** — multi-pass, verification, history/re-run diffing, repo config.
6. **Packaging** — desktop wrapper, self-hosted instance support.

## 9. Open Questions

- Confirm form factor: local web app vs. desktop app vs. hosted service?
- GitHub Copilot as a provider: which API is available/permitted for third-party tools, and how is auth handled?
- Backend language (see §7)?
- How much context beyond the diff is worth the token cost?
- How to evaluate finding quality (eval set of branches with known issues)?
- Single-user local tool, or multi-user/team with shared history later?
