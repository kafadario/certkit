# certkit

A self-hosted study aid for CompTIA Security+ V8 (SY0-801), built on the published exam objectives. It is also how I am learning Spring Boot and AI-assisted development. The why and the plan are in [APPROACH.md](APPROACH.md).

**Status:** M0 — skeleton. See the [milestones](APPROACH.md#7-milestones).

## Run it

Requires Docker.

```bash
cp .env.example .env     # then set POSTGRES_PASSWORD
docker compose up --build
```

Then check that the app and its database are up:

```bash
curl http://127.0.0.1:8080/actuator/health
# {"status":"UP"}
```

The app is published on loopback only, and the database is not published at all. See [docs/security.md](docs/security.md).

## Develop

The Gradle wrapper runs on any JDK 17 or newer and downloads JDK 25 for the build itself. Tests start a real Postgres through Testcontainers, so Docker must be running.

```bash
./gradlew test
```

## Documentation

- [APPROACH.md](APPROACH.md): motivation, learning goals, tools, milestones
- [docs/architecture.md](docs/architecture.md): modules, pages, data ownership
- [docs/security.md](docs/security.md): threat model and the rules that follow from it
- [docs/content-policy.md](docs/content-policy.md): sourcing, attribution, licensing
- [docs/learning/](docs/learning/): my notes on how the stack works, starting with [the Spring and Gradle setup](docs/learning/spring-set-up.md)
- [ai/AI-USAGE.md](ai/AI-USAGE.md): the running record of AI use

## License

Code is licensed under [Apache-2.0](LICENSE). My authored study content in `content/` is licensed under [CC BY-SA 4.0](LICENSE-CONTENT). Third-party material is linked, not included; see [docs/content-policy.md](docs/content-policy.md).
