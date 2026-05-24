# LLM Routing Guide

## The Two-Tab Setup

| Tab | Points to | Cost | Best for |
|-----|-----------|------|----------|
| Tab 1 | Anthropic API (Sonnet / Opus / Haiku) | Paid tokens | Planning, decisions, debugging |
| Tab 2 | Local Qwen2.5 Coder via Ollama | Free (local) | Writing code, boilerplate, CRUD |

**How to use:**
1. Start task in Tab 1 (Sonnet) — get the plan + exact instructions
2. Copy the instruction block to Tab 2 (Qwen) — Qwen writes the code
3. Review the code in Tab 1 if needed — Sonnet reviews, Qwen fixes

---

## Full Task Routing Table

| Task type | Model | Tab | Why |
|-----------|-------|-----|-----|
| Architectural level planning | **Claude Opus** | Tab 1 | Needs deep reasoning, tradeoff analysis |
| Big decisions (DB design, tech choice) | **Claude Opus** | Tab 1 | High stakes, needs nuance |
| Planning a task (breaking into steps) | **Claude Sonnet** | Tab 1 | Good planner, cheaper than Opus |
| Complex bug root cause | **Claude Sonnet** | Tab 1 | Needs to trace logic across files |
| Security review | **Claude Sonnet** | Tab 1 | Needs security knowledge + reasoning |
| Debugging (known error, clear stack trace) | **Claude Haiku** | Tab 1 | Faster + cheaper for straightforward bugs |
| Boilerplate / CRUD code | **Qwen2.5 Coder** | Tab 2 | Free, fast, good at repetitive code |
| Code generation from clear spec | **Qwen2.5 Coder** | Tab 2 | Free, follows instructions well |
| Test generation (only if asked) | **Qwen2.5 Coder** | Tab 2 | Free, mechanical task |
| Refactoring with clear rules | **Qwen2.5 Coder** | Tab 2 | Free, straightforward pattern following |

---

## How to Switch Models (Two-Tab Workflow)

### Tab 1 — Anthropic (Sonnet/Opus/Haiku)
Already configured. Your normal Claude Code.

### Tab 2 — Local Qwen2.5 via Ollama
Open a second terminal. Run:
```bash
# Windows PowerShell
$env:ANTHROPIC_BASE_URL="http://localhost:11434"
$env:ANTHROPIC_API_KEY="ollama"
claude
```

Claude Code in this tab now uses your local Qwen model.

---

## Token Saving Rules
- Use Haiku for anything with a known/clear error — Sonnet is 5x the cost
- Use Qwen for all code generation — it costs nothing
- Use /clear between unrelated tasks — stale context wastes tokens
- Load only the @ai-context/ files relevant to today's task
- Do not run tests unless explicitly needed — each test run costs tokens
