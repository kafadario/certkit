# How the Spring setup works

M0 was generated quickly, mostly by AI. This guide is how I make sure I understand it: what each file does, what Gradle is, how Spring Boot turns a few lines into a running app, and how I would set up the same project again from nothing.

## The pieces at a glance

| File | What it is | Where it came from |
| --- | --- | --- |
| `gradlew`, `gradlew.bat`, `gradle/wrapper/` | The Gradle wrapper: runs a pinned Gradle version without installing Gradle | Spring Initializr |
| `settings.gradle.kts` | Project name, plus the plugin that downloads JDKs | Initializr, then changed |
| `build.gradle.kts` | The build: plugins, Java version, dependencies | Initializr, then changed |
| `src/main/java/.../CertkitApplication.java` | The entry point | Initializr |
| `src/main/java/.../SecurityConfig.java` | CSRF, security headers, no login | Added |
| `src/main/resources/application.properties` | App configuration | Initializr, then changed |
| `src/test/java/...` | Tests, and a Postgres container for them | Initializr, then added to |
| `Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example` | Packaging and running | Added |
| `.github/workflows/ci.yml` | CI | Added |

---

## 1. Gradle

### What it is

Gradle is the **build tool**. It downloads the libraries the project depends on, compiles the Java code, runs the tests, and packages the result into a runnable `.jar`. Without it I would be downloading jars by hand and calling `javac` with a very long classpath.

Its configuration is itself code. This project uses the **Kotlin DSL**, which is why the files end in `.kts`. The older Groovy DSL uses `build.gradle` instead, and Maven, the main alternative to Gradle, uses an XML `pom.xml`. All three do the same job.

Gradle runs on the JVM itself. On my machine it runs on the JDK 17 I have installed; Gradle 9 needs at least Java 17 to run.

### The wrapper

I never installed Gradle. The **wrapper** is a small script that downloads the exact Gradle version the project asks for, then runs it:

- `gradlew` (macOS/Linux) and `gradlew.bat` (Windows) are the scripts I call.
- `gradle/wrapper/gradle-wrapper.properties` pins the version (`gradle-9.7.1`).
- `gradle/wrapper/gradle-wrapper.jar` is the small program that does the downloading. It is committed on purpose, and CI checks that it is the genuine jar.

The first run downloads Gradle into `~/.gradle/wrapper/dists`; later runs reuse it. The point is that my machine, CI, and the Docker build all use exactly the same Gradle, with nothing installed beforehand.

Gradle also starts a background **daemon** on the first build and keeps it alive, so later builds skip the startup cost. That is why the second build is much faster than the first.

### `settings.gradle.kts`

```kotlin
plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "certkit"
```

The settings file is read before the build file. It names the project and, here, adds the **foojay resolver**: when the build asks for a JDK the machine does not have, this plugin downloads one. That is how a machine with only JDK 17 builds a Java 25 project. The downloaded JDK lives in `~/.gradle/jdks`.

### `build.gradle.kts`, section by section

**Plugins** add capabilities to the build:

```kotlin
plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}
```

- `java` teaches Gradle how to compile, test, and package Java code.
- `org.springframework.boot` adds Spring-specific tasks (`bootRun`, `bootJar`, `bootTestRun`) and pins the Spring Boot version for the whole project.
- `io.spring.dependency-management` imports Spring Boot's **BOM** (bill of materials): a curated list of library versions that are tested together. That is why the dependencies below have no version numbers. Boot chooses them, and they are guaranteed to be compatible.

**Identity** of what the build produces:

```kotlin
group = "io.github.kafadario"
version = "0.0.1-SNAPSHOT"
description = "Security+ study aid built on the published exam objectives"
```

`SNAPSHOT` is the convention for "not released yet".

**The Java toolchain:**

```kotlin
java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}
```

This separates *the JDK that runs Gradle* (17 on my machine) from *the JDK that compiles and tests the code* (25). Gradle looks for a JDK 25 and, via foojay, downloads one if it has to.

**Where libraries come from:**

```kotlin
repositories {
	mavenCentral()
}
```

Maven Central is the main public repository of Java libraries.

**Dependencies.** Each line names a library and a **configuration**, which says when that library is needed:

| Configuration | Meaning | Example here |
| --- | --- | --- |
| `implementation` | Needed to compile and to run | the Spring starters |
| `runtimeOnly` | Needed to run, not to compile | the Postgres driver: my code talks to the standard JDBC interfaces, never to driver classes |
| `testImplementation` | Needed to compile and run tests | test starters, Testcontainers |
| `testRuntimeOnly` | Needed only while tests run | the JUnit platform launcher |

The Spring dependencies are **starters**: each one pulls in a coherent set of libraries for one job. `spring-boot-starter-webmvc` brings Spring MVC, an embedded Tomcat server, and JSON support. Each starter has a matching `-test` starter with the testing tools for that area.

| Starter | What it gives certkit |
| --- | --- |
| `spring-boot-starter-webmvc` | Web controllers and the embedded Tomcat server |
| `spring-boot-starter-data-jpa` | Database access through JPA/Hibernate, plus a connection pool |
| `spring-boot-starter-security` | CSRF protection and security headers |
| `spring-boot-starter-actuator` | The `/actuator/health` endpoint |
| `org.postgresql:postgresql` | The driver that speaks the Postgres protocol |
| `spring-boot-testcontainers`, `testcontainers-*` | A real Postgres in Docker for each test run |

**Tests:**

```kotlin
tasks.withType<Test> {
	useJUnitPlatform()
}
```

This tells the `test` task to run tests with JUnit 5.

### Tasks

Everything Gradle does is a **task**, and tasks depend on each other. Running `build` runs everything it needs first.

| Command | What it does |
| --- | --- |
| `./gradlew compileJava` | Compiles `src/main` into `build/classes` |
| `./gradlew test` | Compiles everything and runs the tests; reports go to `build/reports/tests` |
| `./gradlew bootJar` | Packages the app and all its dependencies into one runnable jar in `build/libs` |
| `./gradlew build` | Compiles, tests, and packages: what CI runs |
| `./gradlew bootTestRun` | Runs the app locally with a Testcontainers Postgres, no Compose needed |
| `./gradlew dependencies` | Prints the full dependency tree, including everything the starters pulled in |
| `./gradlew tasks` | Lists the available tasks |
| `./gradlew clean` | Deletes `build/` |

Gradle skips tasks whose inputs have not changed since the last run. When the output says `UP-TO-DATE`, that is what happened.

---

## 2. Spring Boot

### The entry point

```java
@SpringBootApplication
public class CertkitApplication {
	public static void main(String[] args) {
		SpringApplication.run(CertkitApplication.class, args);
	}
}
```

`@SpringBootApplication` combines three annotations:

- `@Configuration`: this class may define beans.
- `@ComponentScan`: find my classes (controllers, services, configuration) in this package and every package under it. That is why every certkit class lives under `io.github.kafadario.certkit`.
- `@EnableAutoConfiguration`: set up everything the dependencies suggest.

### Auto-configuration, and how to override it

Auto-configuration is why so little code is needed. Boot sees the Postgres driver and JPA on the classpath and creates a connection pool and an `EntityManager`. It sees Tomcat and starts a web server on port 8080.

Almost every auto-configured bean is **conditional**: Boot creates it only if I have not defined my own. That is exactly how `SecurityConfig` works:

- Boot's default security would require a login for every request. Defining my own `SecurityFilterChain` bean makes Boot's version step aside.
- Boot would create a default user and print its password in the logs. Defining my own, empty `UserDetailsService` makes that step aside too.

To see every decision Boot made and why, start the app with `--debug`. It prints a conditions evaluation report listing what was configured and what was skipped.

### Configuration

`application.properties` holds settings, but the environment overrides it. Boot maps environment variables onto property names automatically (this is called **relaxed binding**):

```
SPRING_DATASOURCE_URL   →   spring.datasource.url
```

That is why the database address appears nowhere in the code or the properties file. `compose.yaml` supplies it as an environment variable, and in tests Testcontainers supplies it. The same jar runs in both places without changes.

### Actuator

The actuator adds operational endpoints. Only `/actuator/health` is exposed, and it reports `UP` only when the app *and* the database connection are healthy, so it is a real check rather than "the process exists".

---

## 3. Tests

```java
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTests { ... }
```

- `@SpringBootTest` starts the whole application context, just like the real app.
- `@Import(TestcontainersConfiguration.class)` adds a bean that starts a `postgres:18` container in Docker.
- `@ServiceConnection` on that bean tells Boot to take the database URL, username, and password from the running container, so the test needs no connection settings at all.
- `@AutoConfigureMockMvc` provides `MockMvc`, which sends HTTP requests through the whole application (security filters included) without opening a real network port.

`TestCertkitApplication` in the test folder is a second entry point that starts the real app with the test container attached. `./gradlew bootTestRun` uses it, which is the quickest way to run the app while developing.

---

## 4. Packaging and running

The `Dockerfile` has two stages:

1. **Build stage** (`eclipse-temurin:25-jdk`): copies the Gradle files first and downloads dependencies, so that step is cached and only repeats when the build files change. Then it copies the source and runs `bootJar`.
2. **Runtime stage** (`eclipse-temurin:25-jre`): only a Java runtime, the jar, and a non-root user. The compiler, Gradle, and source code are left behind in the first stage.

`compose.yaml` runs that image next to Postgres, and the app waits for Postgres's health check before starting. Why the ports are bound the way they are is in [../security.md](../security.md).

---

## 5. Setting up the same project from scratch

### Step 1: Generate the project

Go to [start.spring.io](https://start.spring.io) and choose:

| Field | Value |
| --- | --- |
| Project | Gradle - Kotlin |
| Language | Java |
| Spring Boot | 4.1.1 (the latest non-SNAPSHOT, non-milestone version) |
| Group | `io.github.kafadario` |
| Artifact / Name | `certkit` |
| Package name | `io.github.kafadario.certkit` |
| Packaging | Jar |
| Java | 25 |
| Dependencies | Spring Web, Spring Boot Actuator, Spring Security, Spring Data JPA, PostgreSQL Driver, Testcontainers |

Click **Generate** and unzip. The same thing from the command line, which is what M0 used:

```bash
curl https://start.spring.io/starter.zip \
  -d type=gradle-project-kotlin -d language=java -d bootVersion=4.1.1 -d javaVersion=25 \
  -d groupId=io.github.kafadario -d artifactId=certkit -d name=certkit \
  -d packageName=io.github.kafadario.certkit \
  -d dependencies=web,actuator,security,data-jpa,postgresql,testcontainers \
  -o starter.zip
```

With Docker running, `./gradlew test` should already pass at this point: Initializr generated a test with a Postgres container.

### Step 2: What M0 changed on top of the generated project

1. **`settings.gradle.kts`**: added the foojay resolver, so machines without JDK 25 can still build.
2. **Testcontainers image**: pinned `postgres:latest` to `postgres:18`, the same version Compose uses, so tests and the app run on the same database.
3. **`.gitignore`**: added `.claude/`, `.env`, and OS clutter.
4. **`application.properties`**: turned off open-in-view and exposed only the health endpoint, without details.
5. **`SecurityConfig.java`**: no login, CSRF on, a Content Security Policy header, and no default user.
6. **`SecurityConfigTests.java`**: tests that pin those security behaviours down.
7. **`Dockerfile`, `.dockerignore`, `compose.yaml`, `.env.example`**: packaging and running.
8. **`.github/workflows/ci.yml`**: build and test on every pull request.

### Without Initializr

Initializr is only a convenience. `gradle init` creates an empty Java project, and adding the three plugins, the toolchain block, and the starters by hand gives the same `build.gradle.kts`. Doing it that way once is a good exercise, because every line then has to be chosen on purpose.

---

## 6. Try it

Small experiments that make the concepts concrete:

- Run `./gradlew dependencies --configuration runtimeClasspath` and find what `spring-boot-starter-webmvc` actually pulled in.
- Delete `useJUnitPlatform()`, run `./gradlew test`, and see how Gradle reacts when it cannot find the JUnit 5 tests. Put it back afterwards.
- Comment out the `UserDetailsService` bean in `SecurityConfig`, run `./gradlew bootTestRun`, and look for the generated password in the log.
- Run the app with `--debug` and read the conditions evaluation report.
- In `compose.yaml`, change the port to the short form `"8080:8080"` and compare the listening address with `netstat -an | findstr 8080`. Then change it back.
