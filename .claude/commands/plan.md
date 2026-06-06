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

STEP 6 — Ask: "Proceed with this plan? yes / modify / cancel"
Do NOT start coding until I say yes.