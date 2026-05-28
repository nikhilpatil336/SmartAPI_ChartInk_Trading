End this session. Run Steps 1–4 automatically. Step 5 asks the user one question — wait for the reply, then continue.

STEP 1 — Update @ai-context/tasks.md
- Mark all completed steps [x]
- If current task is done: move it to Completed Tasks, set the next task as Current Task
- If stopped mid-task: add an entry to "In Progress" with a note on exactly where we stopped

STEP 2 — Update @ai-context/decisions.md (only if a new decision was made today)
Append under the right category: **[Decision]** — [Why]
If no new decisions: skip.

STEP 3 — Update @ai-context/architecture.md (only if structure changed today)
If no structure changes: skip.

STEP 4 — Ask the user ONE question before writing the session doc:
"Which LLMs did you use today?
  Planning: [auto: this session's Claude model]
  Code writing: Sonnet / Qwen / Haiku / same as planning?
  Debugging: Sonnet / Haiku / none?
  (Reply in one line, e.g.: code=Qwen, debug=Haiku)"

Wait for the reply. Use it in Steps 5 and 6.

STEP 5 — Create docs/sessions/[YYYY-MM-DD]-[task-name].md
Write exactly:

## Done today
- [bullet list of what was completed]

## LLM Used
| Phase | Model |
|-------|-------|
| Planning | [this session's Claude model, e.g., Claude Sonnet 4.6] |
| Code writing | [from user's reply in Step 4] |
| Debugging | [from user's reply in Step 4] |

## Decisions made
- [list, or "none"]

## Where we stopped
[If mid-task: exact file + exact state. If done: "Complete."]

## Next step
[First concrete thing to do next session]

STEP 6 — Append ONE new row to @ai-context/llm-log.md:
| [YYYY-MM-DD] | [session name, same as doc filename] | [planning model] | [code model] | [debug model] | [one-word note or —] |

STEP 7 — Print:
"Session saved ✓ — safe to /clear"