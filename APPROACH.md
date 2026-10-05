# certkit — Approach

> **What this file is.** Every project on this account ships with an `APPROACH.md` written *before* the code. It states why the project exists, what I intend to learn from it, how it is built and in what order, and how AI was used. It is the statement of intent — the technical detail lives in [`docs/`](docs/).

**Status:** M0 in progress · **Started:** 2026-08-16 · **v1 target:** CompTIA Security+ V8 (SY0-801)

---

## 1. Why this project exists

I am studying for CompTIA Security+, and I want to get fluent in Java and Spring Boot and in AI-assisted development. Those are two large time commitments that would otherwise compete with each other. certkit is the attempt to make them the same commitment: **the thing I build to study is itself the thing that teaches me the stack.**

This is not a claim that existing study tools are bad or that the world needs another one. Plenty of good ones exist, and if I only wanted to pass the exam I would buy one. The point is the double return — every hour spent modelling exam objectives is an hour spent on the domain material, and every hour spent building the application around it is an hour on Spring Boot and agent-assisted authoring.

The scope is deliberately narrow: one Spring Boot application and one database, run with Docker Compose. Sticking to one framework means the engineering time goes into depth rather than breadth, and more of the total goes into the Security+ material, which is the part with a fixed deadline.

One design constraint comes from the problem itself: **the published exam objectives are the spine.** Nothing exists in the system unless it is anchored to an objective ID that CompTIA actually publishes. Notes, reference links, flashcards, and practice questions all hang off that tree. It keeps my studying honest about coverage, and it gives the software a real data model instead of a bag of flashcards.

> **Scope change, 2026-09-28.** The first version of this plan split the backend into two services, one in Spring Boot and one in .NET, so I could learn both. I narrowed it to a single Spring Boot application before writing any code: two frameworks meant solving the same infrastructure problems twice at half the depth each, while the exam date stayed fixed. .NET moves to a later project on this account.

> **Exam target change, 2026-09-28.** The plan originally targeted Security+ SY0-701. I switched to V8 (SY0-801), which launches on 17 November 2026, while SY0-701 is expected to retire in mid-2027. Studying alongside the build means I will realistically sit the exam in 2027, too close to 701's retirement to be safe. V8's objectives are already published, and a study tool built on them stays current for years instead of months. If CompTIA runs a V8 beta exam I will consider taking it, but the plan does not depend on one.

## 2. What it does

1. **Objective tree** — an animated, navigable hierarchy of SY0-801: 5 domains → objectives → sub-objectives. Each node shows what notes exist, how many questions cover it, and how strong my recall is.
2. **Flashcards** — spaced-repetition cards attached to objective nodes.
3. **Reference library** — curated links to public study material per objective, with my own notes and AI-assisted summaries *of those notes*.
4. **Question bank** — practice items authored against objective text, tagged by objective, difficulty, and provenance.
5. **Practice exams** — assembled to match CompTIA's published domain weightings, then graded, with results reported back onto the tree.

It runs locally with `docker compose up` and is not deployed anywhere.

## 3. What I want to learn

These are first-class deliverables, tracked as seriously as features. A milestone is not finished if the code works but I could not explain why it works.

**Java / Spring Boot** — building a real application rather than following a tutorial: dependency injection and configuration, Spring MVC with server-rendered Thymeleaf pages, data access with JPA and Flyway migrations, validation and error handling, Spring Security for CSRF protection and security headers, and testing with JUnit and Testcontainers against a real Postgres.
*Success looks like:* I can start a new Spring Boot application from empty and explain each layer without reference material.

**Boundaries inside one application** — the app is split into two modules, content and question bank, that talk only through a defined interface and never touch each other's data. That keeps the discipline of a service boundary without the cost of running two services.
*Success looks like:* I can explain where the boundary sits and why, and a test fails if I break it.

**AI agents and AI-assisted development** — where generation genuinely beats writing by hand, and where it quietly produces confident garbage. I expect the question bank to teach me both, by manually verifying every generated question against a cited source rather than against my own recall — I am learning this material, so my judgment alone is not a reliable check. Beyond that: designing a pipeline where AI output is fast but never trusted, and treating prompt and skill design as an engineering artifact with versions and failure modes.
*Success looks like:* I can state, with numbers from M6, where AI saved me time and where it cost me time.

**Everything around the code** — Docker Compose, a CI pipeline, and schema validation as a correctness gate.

**The Security+ material itself** — building the objective tree is a study pass over the syllabus, and writing a question requires actually understanding the objective it maps to. Securing the app itself — network exposure, CSRF, content security policy — puts several exam objectives into practice rather than just on flashcards.

## 4. Tools

| Area | Choice | Why |
| --- | --- | --- |
| Language & framework | Java 25 (LTS) · Spring Boot 4.1 · Gradle 9 | The learning target. |
| Web pages | Spring MVC · Thymeleaf | Everything stays in one app and one build, and the browser only ever talks to one origin. |
| Animated tree | d3-hierarchy, served as a local static file | Handles the tree layout and transitions — one small script, not a frontend framework. |
| Data | Postgres 18 · JPA · Flyway | Realistic database work: versioned migrations, entity mapping, and queries I can actually inspect. |
| Tests | JUnit · Testcontainers | Tests run against a real Postgres rather than an in-memory stand-in. |
| Local run | Docker Compose | The app and Postgres come up with one command from a fresh clone. |
| CI | GitHub Actions | Build, tests, and content validation on every pull request. |
| Content | YAML in git, validated by JSON Schema | Reviewable in pull requests, enforced in CI. |
| AI | Claude Code, with a private configuration | Drafting and review tooling — see §5. |

Module boundaries, pages, data ownership, and rejected alternatives are in [docs/architecture.md](docs/architecture.md).

## 5. How I use AI, and what stays private

I use AI agents heavily here — learning to use them well is one of the stated objectives. What is public is the **record and the governance**; what stays private is the **implementation**.

- `ai/AI-USAGE.md` is a public narrative log: what AI was used for at each milestone, what I accepted, what I overrode, and where it was wrong.
- The review gates are public — schema rules, CI checks, what a pull request has to satisfy.
- `.claude/` is gitignored from the very first commit, before any skill exists, so nothing is ever committed and later removed. It holds skills, agent definitions, and prompts, and none of it is published.

**AI drafts; it never publishes.** A generated question lands in a `draft/` subtree and moves to the reviewed tree only after I check it against the objective and a cited source, confirm the correct answer, and confirm each distractor is wrong for a stated reason. There is a study reason for this beyond correctness: reviewing a question forces me to understand the objective, and a bank I accepted blind would teach me nothing.

Keeping the agent design private does mean a reader sees the outputs and the rules but not the mechanism. That is a deliberate trade — the tooling is reusable across future projects — and `AI-USAGE.md` says so up front rather than leaving anyone wondering where the skills went.

## 6. Ground rules for content

Stated here because they constrain the design; the full reasoning is in [docs/content-policy.md](docs/content-policy.md).

- Objectives are referenced by **ID plus my own paraphrase**. CompTIA's document is not vendored or reproduced in bulk.
- Every question is **authored fresh** against the objective it maps to. Nothing is lifted, reworded, or spun from another provider's bank — and no braindump material is used at any stage, including as AI input.
- Third-party study material (Professor Messer and similar) is **linked, never copied**. Summaries in the app are summaries of *my own notes*, attributed to me, with the source linked.
- Code is Apache-2.0; my authored content is CC BY-SA 4.0.
- These rules are enforced by schema validation in CI, not by discipline.

## 7. Milestones

Each milestone is a pull request against `main` and ends with something runnable. No milestone is "done" until CI is green, `AI-USAGE.md` has its entry, and I can explain the new code without looking it up.

| # | Milestone | Deliverable | Done when |
| --- | --- | --- | --- |
| **M0** | Skeleton | Repo layout, licenses, `.gitignore` (with `.claude/` and `.env`), README, Spring Boot app and Postgres running under Docker Compose with a health check, CI building and testing | `docker compose up` works from a fresh clone; CI green |
| **M1** | Content model | JSON Schemas for objective / note / reference / item; the SY0-801 objective spine across all 5 domains, tagged with the objectives' version and date; domain weights stored as data per exam version; validation wired into CI | Validation fails on a malformed file and passes on the real tree |
| **M2** | Objective tree | Content module: Flyway schema, JPA entities, `content/` loaded into Postgres, server-rendered tree and objective pages with notes and references, Testcontainers tests | Every SY0-801 objective is browsable in the app |
| **M3** | Flashcards | Question bank module: item import, spaced-repetition scheduling, flashcard review pages | A review schedule survives an app restart |
| **M4** | Animated tree | d3 tree layout and transitions, each node showing coverage and recall strength | Can study a domain end to end starting from the tree |
| **M5** | Exam engine | Exam blueprint from domain weights plus weak areas, item selection through the module boundary, exam pages, grading, per-domain results shown on the tree | Generate → sit → score a 90-item mock exam |
| **M6** | Authoring pipeline | AI-assisted drafting into `draft/`, review-gate CLI, provenance in the schema, first reviewed batch of ~150 items | An item cannot reach the reviewed tree without passing the gate |
| **M7** | Presentation | README with screenshots, docs finished, `AI-USAGE.md` complete with M6 numbers, `CONTACT.md`, demo data | A stranger can clone, run, and understand it in under 10 minutes |

M1 doubles as the first full study pass over the syllabus, and M6 as the second and deeper one. That sequencing is intentional: the project timeline and the study timeline are the same timeline.

**Content target for v1:** ~150 reviewed questions (≈30 per domain) and reference coverage on every objective. Enough to sit a real practice exam; not so much that authoring eats the engineering.

## 8. Non-goals for v1

Stated so scope creep has to argue its way in: no authentication or multi-user support; no separate frontend app — Spring renders the pages; no second service or second language; no mobile app; no hosted public demo; no Network+ content, which becomes the second content pack once the engine proves cert-agnostic; no payment, accounts, or telemetry; no AI inference at runtime — all AI work happens at authoring time, offline, gated by review.

## 9. Open questions

- Should reviewed content stay in git and be projected into Postgres on startup, or should the database become authoritative once the item count grows? Git for v1; revisit if authoring becomes painful.
- Per-item provenance fields (`ai_assisted`, `reviewed_by`, `reviewed_at`) — cheap to add, and they would make the review claims machine-checkable rather than narrative. Leaning yes; decide in M1.
- Should the spaced-repetition scheduler also drive exam item selection, or stay confined to flashcards? Separate for now; may converge in M5.
- If the exam date starts crowding the build, content wins and engineering slips. Worth deciding now, before the pressure is real.
- The V8 objectives are a pre-launch release and may still change before 17 November 2026. Because M1 records which version of the objectives each file follows, a revision shows up as a reviewable diff rather than a rewrite.

## Further documentation

- [docs/architecture.md](docs/architecture.md) — modules, pages, data ownership, rejected alternatives
- [docs/security.md](docs/security.md) — threat model, network exposure, CSRF, content security policy
- [docs/content-policy.md](docs/content-policy.md) — sourcing, attribution, and licensing in full
- `ai/AI-USAGE.md` — the running record of AI use (begins at M0)

---

*This document is amended, not rewritten. Superseded decisions keep their entry with a dated note explaining what changed and why.*
