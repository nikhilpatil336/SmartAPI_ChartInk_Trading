Debug this: $ARGUMENTS

---

STEP 1 — What is the exact error?
If no stack trace provided, ask me to paste it before continuing.

STEP 2 — Read the relevant files.
Trace the execution path that causes the error.

STEP 3 — Diagnose:
"Root cause: [X] because [Y]"

STEP 4 — Recommend LLM:
- If root cause is clear and simple → say "Haiku / Qwen can fix this"
- If cause is unclear or spans multiple files → say "Stay on Sonnet"

STEP 5 — Show the fix as a diff BEFORE writing:
```
FILE: src/.../FileName.java
- [broken line]
+ [fixed line]
```
Ask: "Apply fix? yes / no"

STEP 6 — After fix: run mvn compile
If still failing: repeat from Step 2.
Never suppress the error — always fix the root cause.
