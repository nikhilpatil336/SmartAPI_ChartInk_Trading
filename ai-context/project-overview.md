# Project Overview

## Summary
A Spring Boot application (deployed on Raspberry Pi) that acts as a webhook receiver from Chartink alerts. It receives buy/sell signals, validates them against a 5-minute candle SMA-volume strategy, and places intraday MIS orders on the AngelOne SmartAPI broker. It supports both long and short trades, manages stop-loss and exit orders via WebSocket order-status events, and performs an automated EOD square-off at 3:00 PM.

## Tech Stack

| Layer | Technology | Version |
|-------|------------|---------|
| Framework | Spring Boot (WebFlux) | 4.0.0 |
| Language | Java | 21 |
| Reactive HTTP | Spring WebFlux / WebClient | 4.0.0 |
| WebSocket (broker) | Spring WebSocket | 4.0.0 |
| API Docs | SpringDoc OpenAPI (Swagger) | 2.5.0 |
| Build | Maven | 3.x (wrapper) |
| Broker API | AngelOne SmartAPI (REST + WebSocket) | — |
| OTP/TOTP | java-otp + googleauth | 0.4.0 / 1.5.0 |
| JSON | Jackson 2.x + Gson 2.13 | — |
| Excel output | Apache POI | 5.2.5 |
| Config loader | dotenv-java | 3.0.0 |
| Utility | Lombok | — |

## Key Features

1. **Webhook order entry** — Receives Chartink webhook alerts at `/api/order/trade`, `/long/buy`, `/short/sell`; parses stock list + trigger prices.
2. **Two-alert strategy** — First alert caches 5-min candle history; second alert checks candle colour + volume vs SMA-10 to decide long/short/no-trade (within 10-minute window).
3. **Long trade** — Places MIS limit buy; on fill (via WebSocket), places SL-M and limit sell target automatically.
4. **Short trade** — Places MIS limit sell; on fill, places SL-M buy-stop and limit buy-target automatically.
5. **Order event dispatch** — WebSocket feed from broker routes filled/open/rejected status to strategy-specific handlers (fill order strategy pattern).
6. **Aggressive exit** — Cancel-replace loop that reduces price by one tick per attempt to force execution on exit orders.
7. **EOD square-off** — Cron job at 3:00 PM IST Mon-Fri cancels open orders and exits all positions.
8. **Scrip master** — Downloads/caches NSE equity scrip master filtered to F&O stocks; provides symbol-to-token lookup + tick size.
9. **RMS balance** — Fetches available margin from broker on startup; auto-refreshes every 30 min; used for quantity calculation.
10. **Leverage service** — Loads per-stock leverage data; used to calculate max buyable quantity; configurable multiplier in properties.
11. **Order context persistence** — Saves active order context (buy/sell/SL order IDs) to JSON file every 5 min; reloads on restart.
12. **Historical data** — Fetches 5-min candle data from AngelOne for backtest and strategy candle checks.

## Roles & Access
No role-based access. Single-user local application — no authentication layer for incoming webhooks.

## Key commands
```bash
# Run locally (dev)
./mvnw spring-boot:run

# Build JAR
./mvnw clean package -DskipTests

# Run JAR (Raspberry Pi)
java -jar target/local_to_smartapi-0.0.1-SNAPSHOT.jar

# Swagger UI (after start)
http://localhost:8080/swagger-ui.html
```
