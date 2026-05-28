Plan this task (do NOT write any code): $ARGUMENTS

---

STEP 1 — Read @ai-context/architecture.md and @ai-context/decisions.md
Then read the source files relevant to this task.

STEP 2 — List every file that needs to change and why.

STEP 3 — Break the task into atomic steps (5-10 min each).
Number them. Each step: what to do + which file.

STEP 4 — Identify risks or constraints:
- Any decisions.md rules that apply?
- Any edge cases?
- Anything that could break existing features?

STEP 5 — Recommend which LLM for each step:
  [Opus]   — architectural decision needed
  [Sonnet] — reasoning or cross-file debugging needed
  [Haiku]  — clear/simple change with known fix
  [Qwen]   — code generation, boilerplate, template code

STEP 6 — For EACH step marked [Qwen]: immediately after that step, generate a fully filled handoff block using this exact format:

─────────────────────────────────────────────────────────────
  LOCAL LLM TASK — Copy everything below, paste into Tab 2
─────────────────────────────────────────────────────────────
## Task
[one paragraph — what exactly needs to be done, no ambiguity]

## Project constraints (must follow)
- Java 21 + Spring Boot 4 / WebFlux — never call .block() in reactive chains
- Config via ApplicationProperties only — no @Value in business logic
- No JPA/database — file-based JSON persistence only
- OrderService_v2 is active — OrderService is legacy, do not touch
- No System.out.println — use log.info() / log.warn() / log.error()
- Show diff before writing any file. Do not write without showing diff first.
[add any additional rules from CLAUDE.md that apply to this specific task]

## Files to modify
[exact file paths with one-line reason each]

## Exact change required
```diff
FILE: src/main/java/.../FileName.java
- [old line]
+ [new line]
  [context line]
```

## Do NOT touch
[list adjacent files or methods that look related but must stay unchanged]

## Acceptance criteria
[what correct output looks like, or how to verify it worked]
- Compile check: ./mvnw compile — must pass clean
─────────────────────────────────────────────────────────────
  END — switch to Tab 2, paste everything above this line
─────────────────────────────────────────────────────────────

STEP 7 — Ask: "Proceed with this plan? yes / modify / cancel"
Do NOT start coding until I say yes.