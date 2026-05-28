# LLM Attribution Log

Updated automatically by `/wrap-up` at the end of each session.
Backfill older rows from memory — use `unknown` if you don't remember which model wrote the code.

| Date | Session | Planning | Code Writing | Debugging | Notes |
|------|---------|----------|--------------|-----------|-------|
| 2026-05-23 | Bootstrap sessions 1–4 | Sonnet | — | — | Context-only setup, no production code |
| 2026-05-23 | Fallback LTP alert | Sonnet | unknown | — | Multi-file feature |
| 2026-05-23 | Fallback route fix | Sonnet | unknown | Sonnet | Bug: triggerLtpFallback routed wrong |
| 2026-05-24 | Multi-stock fallback batch cancel | Sonnet | unknown | Sonnet | Bug: timer leak on batch alerts |
| 2026-05-25 | EOD analytics revision plan | Sonnet | — | — | Planning only, no code |
| 2026-05-25 | EOD analytics endpoint | Sonnet | unknown | — | New analytics endpoint |
| 2026-05-27 | Entry partial fill qty fix | Sonnet | unknown | Sonnet | Bug: qty=1 instead of qty=3 |
| 2026-05-27 | Scrip master NPE fix | Sonnet | — | Sonnet | Bug: map key was company name not ticker |
| 2026-05-28 | Context workflow improvements | Sonnet | Sonnet | — | Meta-work: llm-log, handoff template, proposed+template folders |
| 2026-05-28 | remainingQty fix + configurable tick size | Sonnet | Haiku | — | Bug: SL overshoot on partial target fill; config: default tick size |

**Legend**
- `—` = not applicable for this phase
- `unknown` = pre-log session; update from memory if known
- `Qwen` = local Ollama (free, no cloud tokens)
- `Sonnet` = Claude Sonnet 4.6
- `Haiku` = Claude Haiku 4.5
- `Opus` = Claude Opus