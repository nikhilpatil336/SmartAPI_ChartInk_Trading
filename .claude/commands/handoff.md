Generate a Local LLM handoff for: $ARGUMENTS

---

STEP 1 — Read the relevant source files for this task.
Identify exactly: which files change, which lines, what the diff looks like, what must not change.

STEP 2 — Output the handoff block, fully filled in, ready to paste.
Do not leave any placeholder unfilled.

─────────────────────────────────────────────────────────────
  LOCAL LLM TASK — Copy everything below, paste into Tab 2  
─────────────────────────────────────────────────────────────
## Task
[one paragraph — what to do, no ambiguity, no "see context"]

## Project constraints (must follow)
- Java 21 + Spring Boot 4 / WebFlux — never call .block() in reactive chains
- Config via ApplicationProperties only — no @Value in business logic
- No JPA/database — file-based JSON persistence only
- OrderService_v2 is active — OrderService is legacy, do not touch
- No System.out.println — use log.info() / log.warn() / log.error()
- Show diff before writing any file. Do not write without showing diff first.
[add any task-specific rules from CLAUDE.md that apply]

## Files to modify
[exact paths — one line each with reason]

## Exact change required
```diff
FILE: src/main/java/.../FileName.java
- [old line]
+ [new line]
  [context line]
```
[if multiple files, repeat the diff block for each]

## Do NOT touch
[list methods/files that look similar but must stay exactly as-is]

## Acceptance criteria
[specific check — what correct output looks like, or what to observe]
- Compile check: ./mvnw compile — must pass clean
─────────────────────────────────────────────────────────────
  END — switch to Tab 2, paste everything above this line
─────────────────────────────────────────────────────────────