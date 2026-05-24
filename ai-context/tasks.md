# Tasks

## Current Task
> Decide on `orderRegistry.remove(ctx)` — enable cleanup after trade closes or archive to separate map
> Started: (next session)


## In Progress
<!-- Nothing mid-way -->

## Next Tasks

### High Priority (correctness / memory risk)

- [ ] **Decide on `orderRegistry.remove(ctx)` after trade completes** — commented out in all four fill strategies (`SellFilledOrderStrategy`, `StopLossFilledOrderStrategy`, `ShortTargetFilledStrategy`, `ShortStopLossFilledStrategy`). Active `OrderContext` entries are never evicted from memory after a trade closes; this will cause stale context to persist between trading days. Decide: enable remove or persist closed trades to a separate archive map.

- [ ] **Re-enable `assertSufficientFunds()` in `chartinkSimpleBuyOrder` / `chartinkSimpleSellOrder`** — commented out at `OrderService_v2.java:336` and `:429`. The balance check only fires for the old v1 path (line 157). New strategy flow has no fund guard. Re-enable when confident in quantity calculation.

### Medium Priority (dead code / cleanup)

- [ ] **Delete `OrderService` (v1) and its orphan services** — `OrderService.java` is entirely dead except for `OrderController.getOrderStatus()` which calls it. `OrderValidationService` and `OrderPollingService` are referenced only from commented-out code inside v1. Steps: migrate `getOrderStatus()` to `OrderService_v2`, then delete all three files and clean `OrderController` import/field.

- [ ] **Purge the giant commented-out block in `OrderService_v2.java`** — lines ~86–683 contain: old v1 buy-order logic, two alternative quantity-calc implementations, a fully commented single-stock `handleAlert/handleFirstAlert/handleSecondAlert`, and multiple interim versions of each method. All active code starts after line 700. This block makes the file ~600 lines longer than it needs to be and is actively confusing to read.

- [ ] **Clean up `FnoUniverseService.java`** — file contains three commented-out class declarations (the entire class defined twice before the live version at line 154). Delete the dead versions. Keep only the active `public class FnoUniverseService` starting at line 154.

- [ ] **Clean up `BuyFilledOrderStrategy.java`** — lines 25–~300 are a commented-out first version of the entire class. The active class starts below it. Delete the commented version; keep only the live one.

- [ ] **Clean up `OrderRegistry.java`** — three earlier implementations of `remove(OrderContext)` are commented out (lines ~40–43, ~120–126, ~204–207). Only the version at line 299 is live. Delete the dead implementations.

- [ ] **Delete `scheduleReconnectWithFreshToken()` in `OrderWebSocketConnector`** — documented as dead code in `decisions.md`; never called; is a duplicate of `scheduleReconnect()`. Safe to delete.

- [ ] **Remove old hardcoded cron comment in `EODSquareOffScheduler`** — lines 28–35 contain the old hardcoded `@Scheduled(cron = "0 0 15 * * MON-FRI")` version that was replaced by the config-driven `${myapp.squareoff-cron}` at line 37. Delete the commented block.

### Low Priority (feature decisions)

- [ ] **Decide fate of JWT periodic refresh** — `@Scheduled` is commented out at `TokenManager.java:96`. The refresh token endpoint was unreliable on AngelOne; current approach does startup-only login. Either document this as a permanent decision in `decisions.md` or re-enable when endpoint is stable.

- [ ] **Enable / remove the trading window gate** — `tradingWindowEnable=false` in `application.properties`; guard checks exist in `OrderController` at lines 53, 81, 124. Either configure time bounds and enable it for live trading, or delete the dead guards to reduce noise.

- [x] **Document + implement fallback LTP alert** — implemented in `OrderService_v2.triggerLtpFallback()`; idempotency via `AtomicReference<AlertState>.compareAndSet`; mock LTP mode added for weekend testing; documented in `decisions.md`.

- [ ] **`BracketOrderRequest.java` is unused** — no references found in active code; likely from an earlier bracket-order experiment. Confirm and delete if truly unreferenced.

## Completed Tasks
- [x] Bootstrap Session 1 — Created ai-context/project-overview.md and filled CLAUDE.md (stack, commands, structure, project-specific rules)
- [x] Bootstrap Session 2 — Created ai-context/architecture.md (folder tree, system design diagram, 4 key data flows, config management)
- [x] Bootstrap Session 3 — Created ai-context/decisions.md (auth, data storage, config, patterns, disabled features)
- [x] Bootstrap Session 4 — Scanned codebase for TODOs, dead code, commented features, known issues; populated real task backlog
- [x] Session 5 — Implemented fallback LTP alert for missed/delayed 2nd alerts; AtomicReference CAS for idempotency; mock LTP test mode; compile verified clean
- [x] Session 6 — Fixed fallback routing: `triggerLtpFallback` now calls `handleAlert` (full path) instead of `handleSecondAlert` directly; removed early CAS from fallback; CAS ownership moved to `handleSingleStockAlert`
- [x] Session 7 — Fixed multi-stock batch fallback leak: `AlertStateRegistry.cancelFallbackIfPending()` + `.doOnSuccess()` in `handleAlert()` cancels remaining stocks' timers when any stock in the batch places an order
