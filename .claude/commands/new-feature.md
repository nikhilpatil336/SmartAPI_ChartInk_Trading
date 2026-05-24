Plan and implement this feature: $ARGUMENTS

This command runs the full workflow: plan → approve → implement → verify → document.

---

**Phase 1 — Break into atomic tasks**
Read @ai-context/architecture.md and @ai-context/decisions.md.
Break this feature into atomic tasks (5-10 min each).
Add them to @ai-context/tasks.md under Next Tasks with [ ] checkboxes.
Show me the task list. Ask: "Does this breakdown look right?"
Wait for approval.

---

**Phase 2 — Plan the first task**
Run /plan on the first task.
Show the full plan. Wait for my approval.

---

**Phase 3 — Implement (task by task)**
For each task:
1. Run /implement [task description]
2. Show diffs, get approval, write code
3. Run tests, confirm passing
4. Mark [x] in tasks.md
5. Say: "Task done. Ready for next task? (yes / pause)"

If I say pause → run /wrap-up automatically.

---

**Phase 4 — After all tasks done**
Run /review to check the full feature diff.
Run /wrap-up to save the session.

---

**LLM routing guidance for this feature:**
Before each task say which LLM to use:
- Architecture / design decisions → "Use Sonnet for this step"
- CRUD / boilerplate / templates → "Local LLM is fine for this step"
- Complex bug or unclear behavior → "Switch to Sonnet"
