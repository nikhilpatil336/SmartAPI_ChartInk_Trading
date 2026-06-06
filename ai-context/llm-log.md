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
| 2026-05-28 | applicationproperties-field-sync | Sonnet | Haiku | — | Cleanup: 12 missing getters/setters + 6 missing fields added |
| 2026-05-31 | backtest-usage-guide | Sonnet | Sonnet | — | Explanation only — no code; backtest framework usage documented |
| 2026-05-31 | backtest-session18 | Sonnet | Sonnet | Sonnet | ATR bug, candle cache fix, charges, takeAllLong, latestEntryTime, full backtest run |
| 2026-05-31 | backtest-audit-fixes | Sonnet | Sonnet | Sonnet | 15+ fixes: stats, bugs, Nifty benchmark, dedup, OOS split, configurable indicators |
| 2026-05-31 | session20-backtest-strategy-enhancement | Opus | Sonnet | — | Chartink strategy decoded; 8 overlay JSONs + Phase 2 engine (PrevDayLevel, CandleStructure, LOW/HIGH ops); spring_faithful.json |
| 2026-05-31 | session21-alert-log-backtest-fix | Sonnet | Sonnet | — | AlertTriggerLogService (parallel log); CURRENT_CLOSE entry fix (AlertEntry + col D price) |
| 2026-05-31 | backtest-capital-monthly | Sonnet | Sonnet | — | Capital simulation columns + monthly breakdown table in SUMMARY sheet |
| 2026-06-02 | analytics-entry-gap-mfe-mae | Sonnet | Sonnet | — | Entry gap direction + MFE/MAE excursion columns added to analytics Excel |
| 2026-06-03 | analytics-header-fix | Sonnet | Sonnet | — | Fix: headers not written to existing sheets; MFE/MAE flags now Yes/No |
| 2026-06-04 | duplicate-entry-orders-fix | Sonnet | Sonnet | Sonnet | Bug: race condition caused duplicate BUY+SL on partial+complete WS events; CAS guard added |
| 2026-06-05 | squareoff-fixes-registry-cleanup | Sonnet | Sonnet | Sonnet | Bug: 3× duplicate cancel at squareoff; CAS guard in squareOff(); maxIdleTime WebClient; registry.remove enabled |
| 2026-06-05 | entry-order-cas-bug-fix | Sonnet | Sonnet | Sonnet | Bug: CAS claimed before delta check in 4 entry strategies; open(filledshares=0) locked CAS, complete event skipped SL+target |
| 2026-06-06 | session31-short-side-test-coverage | Sonnet | Sonnet | — | Short-side mirror tests: ShortTargetOpen, ShortStopLossOpen, SquareOff D5, E3/E4 error paths. 46/46 green |
| 2026-06-06 | test-suite-second-pass-audit | Sonnet | Sonnet | — | eq(qty) verifies + full-fill path tests; 49/49 green |
| 2026-06-06 | session34-test-coverage-gaps | Sonnet | Sonnet | — | C7/SC7 full-fill SL open tests; LONG entry guard S5; longBuyOpenNotPartialFilled factory; 52/52 green |

**Legend**
- `—` = not applicable for this phase
- `unknown` = pre-log session; update from memory if known
- `Qwen` = local Ollama (free, no cloud tokens)
- `Sonnet` = Claude Sonnet 4.6
- `Haiku` = Claude Haiku 4.5
- `Opus` = Claude Opus