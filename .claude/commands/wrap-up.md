End this session. Run these steps automatically, no asking.

STEP 1 — Update @ai-context/tasks.md
- Mark all completed steps [x]
- If current task is done: move to Completed, set next task as Current
- If stopped mid-task: add to "In Progress" with note on where we stopped

STEP 2 — Update @ai-context/decisions.md (only if a new decision was made today)
Append under the right category: **[Decision]** — [Why]
If no new decisions: skip.

STEP 3 — Update @ai-context/architecture.md (only if structure changed today)
If no structure changes: skip.

STEP 4 — Create docs/sessions/[YYYY-MM-DD]-[task-name].md
Write:
## Done today
- [bullet list]

## Decisions made
- [or "none"]

## Where we stopped
[If mid-task: exact file + exact state]

## Next step
[First thing to do next session]

STEP 5 — Print:
"Session saved ✓ — safe to /clear"
