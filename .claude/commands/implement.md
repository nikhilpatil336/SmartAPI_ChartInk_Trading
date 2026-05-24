Implement this: $ARGUMENTS

---

STEP 1 — Read relevant files first. Do not skip.

STEP 2 — Before touching any file, show me:
```
FILE: src/.../FileName.java
REASON: [one sentence why this file needs to change]

- [removed line]
+ [added line]
  [context line]
```
Then ask: "Approve? yes / no / add context"
Wait for my reply. Do not write anything yet.

STEP 3 — Only after I say "yes": write the file.
After writing, just say "Done ✓" — do not show the code again.

STEP 4 — Run: mvn compile
If it fails, show the error and fix it. Do not ask.

STEP 5 — Update @ai-context/tasks.md
Mark this step as [x]. If the task is complete, move it to Completed Tasks.

STEP 6 — Print summary:
• What changed (2-3 bullets)
• Why
• Which LLM should do the next task (Sonnet / Haiku / Qwen)
