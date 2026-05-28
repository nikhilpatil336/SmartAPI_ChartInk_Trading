# Quick Reference — local_to_smartapi

## Commands
```bash
./mvnw spring-boot:run                       # run locally
./mvnw compile                               # fast compile check
./mvnw clean package -DskipTests             # build JAR
java -jar target/local_to_smartapi-0.0.1-SNAPSHOT.jar  # run JAR on Pi
http://localhost:8080/swagger-ui.html        # Swagger UI
```

## Navigate to… (key files by function)

| Need to change... | File |
|-------------------|------|
| Trade params (SL %, target %, qty) | `application.properties` → `myapp.*` |
| Add/rename a config field | `ApplicationProperties.java` + `application.properties` |
| Entry order logic (which candle condition) | `OrderService_v2` → `handleSecondAlert()` |
| What happens after a BUY fills | `eventhandling/fillorderstrategy/BuyFilledOrderStrategy.java` |
| What happens after SHORT SELL fills | `eventhandling/fillorderstrategy/ShortEntryFilledStrategy.java` |
| Partial fill / open order handling | `eventhandling/openorderstrategy/` |
| Aggressive exit loop | `exit/AggressiveExitManager.java` |
| EOD square-off time | `application.properties` → `myapp.squareoff-cron` |
| Add a REST endpoint | `controller/OrderController.java` |
| WebSocket reconnect | `websocket/OrderWebSocketConnector.java` |
| Scrip master (symbol → token) | `service/ScripMasterService.java` |
| Order context persistence | `storage/OrderContextStorageService.java` |

## Active vs legacy
- `OrderService_v2` — active (all new code goes here)
- `OrderService` — legacy (do NOT add to this)

## File paths (two sets in application.properties)
- **Windows block**: uncomment for local dev, comment out Pi block
- **Raspberry Pi block**: uncomment for production, comment out Windows block
- Manual toggle only — no Spring profile switching

## Tab guide
| Task | Tab | Model |
|------|-----|-------|
| Architecture / big decisions | Tab 1 | Opus |
| Planning a task | Tab 1 | Sonnet |
| Known error with stack trace | Tab 1 | Haiku |
| Writing code from a clear plan | Tab 2 | Qwen (local, free) |

## LLM attribution log
`ai-context/llm-log.md` — updated automatically by `/wrap-up` each session