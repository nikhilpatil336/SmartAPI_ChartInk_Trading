# local_to_smartapi
# Spring Boot webhook server that receives Chartink alerts and places intraday MIS orders on AngelOne SmartAPI using a 5-min SMA-volume strategy.

## Stack
- Java 21, Spring Boot 4.0.0 (WebFlux / reactive)
- Spring WebSocket (broker WebSocket client)
- Jackson 2.x + Gson 2.13, Lombok, Apache POI 5.2.5
- dotenv-java for secrets, java-otp + googleauth for TOTP login
- SpringDoc OpenAPI 2.5.0 (Swagger UI)
- Maven 3.x (wrapper)

## Key commands
```bash
# run locally:
./mvnw spring-boot:run

# build JAR:
./mvnw clean package -DskipTests

# run JAR on Raspberry Pi:
java -jar target/local_to_smartapi-0.0.1-SNAPSHOT.jar

# Swagger UI:
http://localhost:8080/swagger-ui.html
```

## Project structure
```
src/main/java/.../
  controller/        REST endpoints (order, login, orderbook, scrip, websocket, test, historic)
  service/           Business logic (OrderService_v2 is the active one; OrderService is legacy)
  eventhandling/     WebSocket order event queue, dispatcher, fill/open/rejected handlers (strategy pattern)
  exit/              AggressiveExitManager, SquareOffManager, ExitCoordinator, ExitType
  storage/           File-based persistence (tokens, scripmaster, RMS, leverage, order context, SL store)
  registry/          In-memory stores (OrderRegistry, AlertStateRegistry)
  factory/           OrderRequestFactory — builds broker API request objects
  properties/        ApplicationProperties (@ConfigurationProperties prefix="myapp") — central config
  websocket/         Broker WebSocket connector + order status handler
  scheduler/         EODSquareOffScheduler, OrderContextPersistenceScheduler
  startupservice/    ApplicationStartupService — runs on ready event; loads all data
src/main/resources/
  application.properties  All config (file paths, trading params, leverage, exit strategy)
  logback-spring.xml
data/order-context/  JSON files for persisted order context (one file per day)
```

## Project-specific rules
- **`OrderService` is legacy — never add new logic to it.** All active trading logic lives in `OrderService_v2`. The old service is kept only because `OrderController.getOrderStatus()` still references it.
- **All config must come from `ApplicationProperties` (prefix `myapp.*`) — never use `@Value` for business logic.** The only allowed `@Value` usage is in `ApplicationStartupService` for `slOrderBaseDir` (a known exception).
- **No JPA/database — never add `@Entity`, `@Repository`, or any DB dependency.** All persistence is file-based JSON via `storage/` services.
- **The two-alert strategy flow (handleAlert → handleFirstAlert → handleSecondAlert) is the active entry point.** Direct `/long/buy` and `/short/sell` endpoints bypass the strategy and are for manual/debug use only — do not route new strategy logic through them.
- **`mapToOrderResult()` in `OrderService_v2` has status hard-coded to `"COMPLETE"` (line ~235).** This is intentional for the current trading mode — do not remove it without understanding the full polling flow.
- **The fallback LTP alert feature is disabled (`fallbackAlertEnable=false`).** The scheduler code inside `handleFirstAlert` is commented out — do not re-enable without explicit instruction.
- **Secrets (`ANGEL_CLIENT_ID`, `ANGEL_PASSWORD`, `ANGEL_TOTP`, `ANGEL_PRIVATE_KEY`) come from the `.env` file via dotenv-java — never hardcode them or load from `application.properties` directly.**
- **File paths in `application.properties` have two sets: Windows (commented out) and Raspberry Pi (active).** When running locally, manually comment/uncomment the correct block — there is no profile-based path switching.

## Behavior rules (same for every project — do not change)
- ALWAYS show me what changes you will make BEFORE making them
- Wait for my "yes" / "no" / "add context" before writing any file
- After writing: do NOT show the changes again — just confirm "Done ✓"
- Explain only what changed, in 2–5 bullet points. No preambles.
- Do NOT generate or run test cases unless I explicitly ask
- After every answer, explicitly show:
  - active model/provider used for the response
  - whether response came from local LLM or cloud model
  - estimated token usage for the response
- If exact token usage is unavailable, clearly label it as estimated tokens
- If hybrid routing is used, explicitly mention which model generated the final answer
- Never hide or omit model identity in responses involving code changes or planning

## LLM routing (same for every project — do not change)
Full table → @ai-context/llm-routing.md

Quick reference:
- Architecture / big decisions     → Claude Opus   (Tab 1)
- Task planning                    → Claude Sonnet (Tab 1)
- Complex bug root cause           → Claude Sonnet (Tab 1)
- Security review                  → Claude Sonnet (Tab 1)
- Debugging (known error)          → Claude Haiku  (Tab 1)
- Boilerplate / CRUD code          → Qwen2.5 Coder (Tab 2)
- Code generation from clear spec  → Qwen2.5 Coder (Tab 2)
- Test generation (if asked)       → Qwen2.5 Coder (Tab 2)

## Context files (load only what today's task needs — do not load all)
- Overview  → @ai-context/project-overview.md
- Structure → @ai-context/architecture.md
- Decisions → @ai-context/decisions.md
- Tasks     → @ai-context/tasks.md
- LLM guide → @ai-context/llm-routing.md
