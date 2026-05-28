# Qwen Handoff — [TASK NAME]

**Usage:** When Claude says "Qwen can handle this", ask Claude to fill in this template.
Then paste the filled version into Tab 2 (Qwen / Ollama).

---

## Project constraints (always apply)
- Java 21 + Spring Boot 4 / WebFlux reactive — never call `.block()` in reactive chains
- Config via `ApplicationProperties` bean only — no `@Value` in business logic
- No JPA/database — persistence is file-based JSON in `storage/` services only
- `OrderService_v2` is active — `OrderService` is legacy, do not modify it
- No `System.out.println` — use `log.info()` / `log.warn()` / `log.error()`
- Show a diff BEFORE writing any file. Do not write without showing the diff first.

## Task
[Claude fills this in — one paragraph describing exactly what needs to change]

## Files to modify
```
src/main/java/.../FileName.java            — [what section to change and why]
src/main/resources/application.properties — [only if new config key needed]
```

## Exact change required
```diff
FILE: src/main/java/.../FileName.java
- [old line]
+ [new line]
  [context line]
```
[Claude fills this — exact diff or clear before/after. Be specific about line context.]

## Do NOT touch
- [Claude lists methods/files that must stay exactly as-is]
- [Include any adjacent code that looks similar but must not change]

## Acceptance criteria
- [Claude lists what correct output looks like]
- Compile check: `./mvnw compile` passes clean

## After writing
Run: `./mvnw compile`
If it fails: fix the root cause. Do not suppress.
Then say: "Done ✓ — compile clean"