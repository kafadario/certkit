# AI usage log

certkit is built with heavy use of AI agents (Claude Code). This file is the public record of that use: what AI did at each milestone, what I accepted, what I overrode, and where it was wrong.

The tooling itself (skills, agent definitions, prompts) lives in `.claude/`, which is gitignored and never published. That is a deliberate trade, explained in [APPROACH.md §5](../APPROACH.md#5-how-i-use-ai-and-what-stays-private).

---

## Planning

**What AI did.** Drafted `APPROACH.md` and the three documents in `docs/` from my brief, and answered my questions about the design as I reviewed it.

**What I overrode.**
- The first draft justified using Java and .NET by how often they appear in job postings. That was not my reason; I rewrote the motivation around learning the frameworks and preparing for the exam at the same time.
- AI proposed a running Spring-vs-.NET comparison document. I declined it.
- I narrowed the project from two services (Spring Boot and .NET) to a single Spring Boot application, and switched the exam target from SY0-701 to V8 (SY0-801).
- I had the approach file stripped back to intent, with the technical detail moved into `docs/`.

**Where AI was wrong or unclear.**
- It described the two services as "building the same class of thing twice", which read as building the same application twice. I had to ask what it meant.
- Its first architecture diagram showed a published port on an internal service, contradicting the security rules it had just written.
- The service contract returned only item IDs, which would have made grading an exam impossible. AI caught this itself while rewriting the architecture.
- When I asked why the question bank would reveal where AI fails, it pointed out a gap in its own plan: I would be reviewing questions on material I am still learning, so the review gate now requires checking each question against a cited source.

## M0 — Skeleton

**What AI did.** Generated the project from Spring Initializr, looked up current versions rather than guessing them, and wrote the Compose file, Dockerfile, CI workflow, security configuration, and tests.

**What I decided.** Java 25, the root package `io.github.kafadario.certkit`, and to start Docker myself. I also set up a separate git identity for this account, so portfolio commits use its private GitHub email instead of my personal one.

**What I overrode.**
- AI generated and verified the whole skeleton in one pass. That gave me a working build quickly, but I could not have explained it, and understanding it is the point of this project. I had it write [docs/learning/spring-set-up.md](../docs/learning/spring-set-up.md): what Gradle is and how it works, how Spring Boot configures itself, and how to set up the same project from scratch.

**Where AI was wrong.**
- The request it sent to Spring Initializr turned the `+` in "Security+" into a space. It noticed and fixed this.
- Its first check that Postgres was not reachable from my machine was inconclusive. A second check found that port 5432 belonged to a separate PostgreSQL install on my machine, not to certkit.
