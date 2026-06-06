# Architecture

## Folder structure

```
src/main/java/.../
  controller/          REST endpoints — order entry, login, orderbook, scrip, websocket debug, historic
  service/             Business logic — OrderService_v2 (active), LoginService, RmsService, ScripMasterService, etc.
  eventhandling/       WebSocket order event queue + dispatcher + fill/open/rejected handlers (strategy pattern)
    fillorderstrategy/   Per-order-type fill handlers (long: buy/sell/SL; short: entry/SL/target)
    openorderstrategy/   Per-order-type open handlers (same split)
    orderstatushandler/  Top-level status routers: FilledOrderHandler, OpenOrderHandler, RejectedOrderHandler
  exit/                AggressiveExitManager, SquareOffManager, ExitCoordinator, ExitType
  execution/           CancelReplaceExecutor (tick-step price reduction), OrderActionExecutor
  factory/             OrderRequestFactory — builds broker API request objects from internal models
  registry/            In-memory stores: OrderRegistry (active orders), AlertStateRegistry (two-alert state)
  storage/             File-based persistence: tokens, scrip master, RMS, leverage, order context, SL store, AlertTriggerLogService (alert pair log for manual Excel import)
  scheduler/           EODSquareOffScheduler (3 PM cron), OrderContextPersistenceScheduler (5 min)
  startupservice/      ApplicationStartupService — loads all data on ready; ShutdownHandler
  websocket/           Broker WebSocket connector + order status handler; test/ has dummy/stub connector
  historicdata/        HistoricalDataService + models for 5-min candle fetch and backtest; Candle.getTime() is @JsonIgnore (derived from timestamp string)
  backtest/runner/     BacktestRunner, BacktestResult, BacktestSummary, AlertEntry (record: triggeredAt + stock + firstAlertPrice read from Excel col D)
  analytics/           EOD analytics pipeline: AlertAnalyticsService (reads alerts Excel, fetches candles, builds rows), AlertAnalyticsRow (40-col POJO), TargetSlEvaluator (target/SL/AUTO_SQUAREOFF), AlertAnalyticsExcelWriter (monthly workbook, 3 sheets), EODAnalyticsScheduler (@5 PM)
  marketdata/          MarketDataService / SmartApiMarketDataService — LTP quotes
  properties/          ApplicationProperties (@ConfigurationProperties prefix="myapp") — single config bean
  client/              BrokerApiClient — low-level WebClient wrapper for AngelOne REST API
  config/              WebClient configs, TokenManager, SwaggerConfig
  model/               DTOs and request/response POJOs (no JPA entities)
src/main/resources/
  application.properties   All tunable config (file paths, trading params, leverage, exit strategy)
  logback-spring.xml
data/
  order-context/       Daily JSON files for persisted order context (one per trading day)
```

---

## System design

```
  Chartink Alert
       │  POST /api/order/trade
       ▼
  OrderController
       │
       ▼
  OrderService_v2 ──► AlertStateRegistry (two-alert state per symbol)
       │                    │
       │              First alert: cache candle history
       │              Second alert: validate candle + volume vs SMA-10
       │
       ├──► OrderCalculationService  (qty = margin / price / leverage)
       ├──► ScripMasterService       (symbol → token, tick size)
       ├──► RmsService               (available margin)
       ├──► LeverageService          (per-stock leverage)
       │
       ▼
  OrderActionExecutor / BrokerApiClient ──► AngelOne REST API
       │                                        │
       │                                   Order placed
       │
       ▼
  OrderRegistry  (stores buy/sell/SL order IDs in memory)
       │
       ▼
  OrderWebSocketConnector ◄── AngelOne WebSocket (order status feed)
       │
       ▼
  OrderStatusWebSocketHandler
       │
       ▼
  OrderEventQueue ──► OrderEventDispatcher
                            │
              ┌─────────────┼─────────────┐
              ▼             ▼             ▼
      FilledHandler   OpenHandler   RejectedHandler
              │
    (strategy pattern — selects by order role: entry/SL/target)
              │
              ▼
      Place SL + Target orders  OR  AggressiveExitManager
              │
              ▼
        CancelReplaceExecutor (tick-step price loop until filled)

  Schedulers (background):
    EODSquareOffScheduler        @ 3:00 PM IST Mon-Fri → SquareOffManager
    OrderContextPersistenceScheduler @ every 5 min    → OrderContextStorageService
```

---

## Key data flows

**1. Two-alert strategy (webhook → order placement)**
```
POST /api/order/trade (Chartink webhook)
  → OrderService_v2.handleAlert(symbol, price)
  → AlertStateRegistry: state == NONE?
      YES → handleFirstAlert: fetch 5-min candles → store in registry → state = FIRST_ALERT
      NO  → handleSecondAlert (within 10-min window):
              check candle colour (green/red) + volume > SMA-10
              → decide: LONG / SHORT / NO_TRADE
              → place MIS limit entry order via BrokerApiClient
              → store OrderContext in OrderRegistry
```

**2. Long trade fill → SL + target placement**
```
AngelOne WebSocket pushes order status event
  → OrderStatusWebSocketHandler.handleMessage()
  → OrderEventQueue.enqueue(event)
  → OrderEventDispatcher.dispatch(event)
  → FilledOrderHandler.handle(event)
  → looks up OrderRegistry by orderId → role = BUY_ENTRY
  → BuyFilledOrderStrategy.execute():
      place SL-M order (stop-loss)
      place limit SELL order (target)
      update OrderContext with new SL + target IDs
```

**3. Aggressive exit (target not filling)**
```
ExitCoordinator.triggerExit(symbol, ExitType.AGGRESSIVE)
  → AggressiveExitManager.startExit(orderContext)
  → CancelReplaceExecutor loop:
      cancel existing sell order
      re-place sell at (last_price − 1 tick)
      wait for fill event
      if not filled → repeat (price − 1 tick)
      until filled or price floor reached
```

**4. EOD square-off**
```
EODSquareOffScheduler fires @ 3:00 PM IST (Mon-Fri cron)
  → SquareOffManager.squareOffAll()
  → for each active OrderContext in OrderRegistry:
      cancel open SL order
      cancel open target order
      place MIS market sell (long) or market buy (short) to flatten position
  → clear OrderRegistry
  → OrderContextStorageService.persist() (final snapshot)
```

---

## Config management

| What | Where | How loaded |
|------|-------|------------|
| Secrets (client ID, password, TOTP, private key) | `.env` file (not committed) | `dotenv-java` loads on startup into system properties |
| All business config (paths, trade params, leverage multiplier) | `application.properties` prefix `myapp.*` | `@ConfigurationProperties` → `ApplicationProperties` bean |
| File paths (Windows vs Raspberry Pi) | Two blocks in `application.properties` | Manual comment/uncomment — no Spring profiles |
| Auth token (JWT) | `data/token/` JSON file | `TokenStorageService` reads/writes; `TokenManager` holds in memory |
| Broker API base URL | `BrokerApiProperties` | `@ConfigurationProperties` prefix `broker.*` |

No encryption at rest. Secrets stay in `.env` on the local machine / Pi only.
