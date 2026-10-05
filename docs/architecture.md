# Architecture

Detail extracted from `APPROACH.md`. This is the technical reference; the approach file states the intent.

## Shape

One Spring Boot application and one Postgres database, run together with Docker Compose. Inside the application there are two modules with an enforced boundary between them — a *modular monolith*.

## Repo layout

```
certkit/
├─ src/main/java/io/github/kafadario/certkit/
│  ├─ content/              objective tree, notes, references, exams, progress
│  └─ questionbank/         items, selection, spaced-repetition scheduling
├─ src/main/resources/
│  ├─ templates/            Thymeleaf pages
│  ├─ static/               CSS, the tree script, vendored d3
│  └─ db/migration/         Flyway migrations
├─ src/test/java/           JUnit + Testcontainers
├─ content/                 YAML source of truth (objectives, notes, references, items)
├─ schemas/                 JSON Schema for every content type — CI-enforced
├─ docs/                    this directory
├─ ai/                      AI-USAGE.md — public narrative decision log
├─ .github/workflows/       CI: build, test, container image
├─ compose.yaml             app + Postgres
├─ .env.example             database settings template (.env itself is gitignored)
├─ Dockerfile
├─ build.gradle.kts
└─ .claude/                 GITIGNORED — never published
```

Each module package owns its own controllers, services, repositories, and entities. The root package is `io.github.kafadario.certkit`.

## Request flow

```
browser ──HTTP──> certkit app ──JDBC──> Postgres
       127.0.0.1:8080                   no published port
      (loopback only)                 (compose network only)

inside the app:
   content ────QuestionBank interface────> questionbank
   (schema: content)                       (schema: questionbank)
```

The browser talks to a single origin. Pages are rendered server-side by Thymeleaf; the only JSON endpoint is the one the d3 tree script reads. Only the app is reachable from the host, and only over loopback — see [security.md](security.md).

## Module responsibilities

No entity, table, or page belongs to both modules. `questionbank` has never heard of a domain weighting, an exam, or a study note; `content` has never heard of SM-2.

| | **content** | **questionbank** |
| --- | --- | --- |
| Owns | Study material, exams, progress | Items and review scheduling |
| Pages | `GET /` — objective tree<br>`GET /objectives/{id}`<br>`GET /exams/new`, `POST /exams`<br>`POST /exams/{id}/submit`<br>`GET /progress` | `GET /flashcards`<br>`POST /flashcards/{id}/grade` |
| JSON | `GET /api/tree` — data for the d3 script | — |
| Entities | `Domain`, `Objective`, `Note`, `Reference`, `ExamAttempt`, `AttemptAnswer`, `DomainScore` | `Item`, `Choice`, `ItemTag`, `ReviewState` |
| Postgres schema | `content` | `questionbank` |
| Knows nothing about | SM-2 intervals, item selection strategy | Exams, domain weights, notes, references |

### The boundary

`questionbank` exposes one interface, `QuestionBank`, and `content` uses nothing else from it:

- **select items for a blueprint** — takes `[{objectiveId, count, difficultyMix}]`, returns the chosen items;
- **read items** — returns read-only views (stem, choices, correct choice) so `content` can render an exam and grade it.

`content` never touches `questionbank`'s entities, repositories, or tables, and the reverse holds too. Each module's tables live in its own Postgres schema, so a cross-module join is impossible to write by accident.

The rule is checked by a test rather than by discipline. Spring Modulith's module verification or an ArchUnit rule both do this; which one is decided in M2.

### Content loading

On startup the app validates `content/` against `schemas/` and upserts it into Postgres. The database is a projection of the YAML in git, never the source of truth. User state — review schedules, exam attempts, scores — lives only in the database.

## Why this shape

| Decision | Reasoning |
| --- | --- |
| One Spring Boot application | Learning-driven. One framework learned in depth beats two learned halfway, and the time saved goes into the Security+ material. |
| Two modules with an enforced boundary | Keeps the design lesson of a service boundary — explicit contract, separate data — without the cost of running, deploying, and wiring two services. Content and items also change at different rates, so the split holds up on its own terms. |
| Thymeleaf over a separate frontend app | One build, one language, one origin. The learning time stays on Spring instead of spreading into a second toolchain, and cross-origin concerns disappear entirely. |
| d3 for the tree only | The animated tree is the one piece that needs real client-side layout and transitions. A single vendored script does it; the rest of the UI is plain server-rendered HTML with CSS. |
| Postgres in Compose over an embedded database | Realistic migrations and queries, and Testcontainers lets tests hit the same database the app uses. An in-memory database in tests would hide Postgres-specific behaviour. |
| One database, one schema per module | Data ownership per module without the overhead of a second database. |
| Flyway | Schema changes are versioned in git next to the code that needs them. |
| Content as YAML in git | Reviewable in pull requests, diffable, and provenance is enforced by schema before anything reaches the database. |

## Rejected alternatives

- **Two services, Spring Boot and .NET** — the original plan (see the scope change in `APPROACH.md` §1). It solved the same infrastructure problems twice at half the depth each.
- **Microservices with a message bus** — one user and two modules do not need Kafka. Building infrastructure the problem does not call for is the failure mode `APPROACH.md` exists to prevent.
- **Separate frontend app (Svelte, React)** — a second build, a second language, and cross-origin configuration, all to learn things that are not this project's goal.
- **Embedded database (H2, SQLite)** — simpler to run, but teaches less and behaves differently from Postgres.
- **Static site, content compiled to JSON** — would ship fastest and deploy free, but leaves no backend to learn on.
- **Auth / multi-user** — explicit non-goal for v1. Worth learning, but it would consume a milestone and teach less than the exam engine or the authoring pipeline will.
