# Security posture

Detail extracted from `APPROACH.md`.

## Threat model

certkit v1 is a **single-user application, running on one machine, with no authentication**. That is a deliberate choice, and it is only defensible because of the boundary it depends on: nothing in the stack is reachable from outside loopback. Every rule below exists to hold that boundary or to cover the one path around it — a web page in my own browser sending requests to the app.

## Constraints M0 must satisfy

| Constraint | Why |
| --- | --- |
| The app publishes as `127.0.0.1:8080:8080` — never the short `8080:8080` form | Docker's short form binds `0.0.0.0`, i.e. every interface including Wi-Fi. Explicit loopback keeps traffic on the machine. |
| Postgres gets **no** `ports:` entry | The app reaches the database by service name over the Compose network. Publishing exists only to let the host in, and nothing on the host needs the database. |
| Postgres credentials come from a gitignored `.env`; `.env.example` is committed | A hardcoded password in a public repo is bad practice made visible to exactly the audience this account is for. |
| CSRF protection stays on for every state-changing request | See *CSRF, not CORS* below. |
| `GET` requests never change state | CSRF protection covers `POST`, `PUT`, and `DELETE`. A `GET` that writes would sit outside it. |
| A Content Security Policy allowing scripts from the app's own origin only; d3 is vendored, not loaded from a CDN | Nothing runs in the page that the app did not serve itself. It also keeps the app working offline. |
| Rendered content is always escaped — `th:text`, never `th:utext` | Notes and questions are data, and some of it is AI-drafted. Unescaped rendering would turn a malformed or malicious draft into script in the page. |
| Bean Validation on every form and request body | Bad input is rejected at the edge. |

## CSRF, not CORS

An earlier design had a separate frontend on its own origin, which made CORS the browser-side concern. With Spring rendering the pages, the browser only ever talks to one origin, so no CORS configuration is needed — and none should be added, because Spring's default of allowing no cross-origin reads is the safe one.

The browser-side threat that remains is **cross-site request forgery**. The app has no login, so any request that reaches it is trusted. A page on some other site, open while certkit is running, could auto-submit a hidden form to `http://127.0.0.1:8080/...` and, for example, wipe my exam history. A form `POST` is a "simple" request, so no preflight stops it.

Spring Security's CSRF protection requires a per-session token on every state-changing request. Thymeleaf adds the token to forms automatically, and a forged request from another site cannot know it. Browsers are also adding protections against public sites reaching local addresses, but support varies and the app should not rely on them.

Spring Security is on the classpath for CSRF protection and its default security headers, **not for login**. Its default configuration also switches on a generated login page, so certkit's security configuration replaces that with `permitAll()` while keeping CSRF and the headers.

## Why HTTP and not HTTPS

TLS protects data in transit from an attacker positioned on the network path. In this deployment there is no such path: the browser→app hop is loopback, and the app→Postgres hop stays inside the Docker bridge network. The packets never reach a network interface, so the control does not map to a threat.

It is not free, either. Local HTTPS means a self-signed certificate, which means routinely clicking through browser certificate warnings — a worse habit than the plaintext loopback traffic it would replace.

The choice is not "HTTPS is too restrictive." It is that HTTPS is the wrong control for this threat model, and the right controls are the rules above.

## Docker networking: which `127.0.0.1` is which

Each container has its own **network namespace**, so it has its own loopback and its own bridge IP. `127.0.0.1` therefore means different things depending on where it is written: inside the app container it means the app container, inside the Postgres container it means Postgres, and on the host it means the machine.

Three consequences:

**The app binds `0.0.0.0` inside its container.** Port publishing forwards to the container's *bridge* IP, not its loopback. Spring Boot binds all interfaces by default, so `server.address` is simply left unset. Setting it to `127.0.0.1` would make the app listen where Docker does not deliver, giving connection refused despite the app running fine. Binding `0.0.0.0` inside a container is not the risk it is on a bare host — the namespace only contains the bridge interface.

**The address in a `ports:` entry is the host side.** In `127.0.0.1:8080:8080`, the IP selects which *host* interface Docker listens on. It has nothing to do with the container's internal binding.

**The app reaches Postgres by service name, never localhost.** Compose runs a DNS resolver on its networks.

```
app       →  server.address unset (binds 0.0.0.0 inside the container)
             ports: ["127.0.0.1:8080:8080"]
             SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/certkit

postgres  →  no ports: entry at all
```

```
OK    jdbc:postgresql://postgres:5432/certkit     resolves to the Postgres container
WRONG jdbc:postgresql://localhost:5432/certkit    hits the app container's own loopback → refused
```

## When this design stops being valid

The moment the app is reachable beyond loopback — LAN exposure, a tunnel, a VPS, "let me study from my laptop" — this posture is void. At that point the requirement is TLS **and** authentication together, and adding TLS alone would be the less important half. Recorded here as a boundary to cross deliberately, not to drift across.

*For anyone running this on Linux:* Docker's published ports install their own NAT rules that are traversed before the host firewall's input chain, so a `ufw deny` will not block a published port the way you would expect. Another reason the binding is explicit rather than firewalled after the fact.
