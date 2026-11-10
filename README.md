# Spring Integration Sample — Order Processing Pipeline

A Spring Boot 4.1.0 + Spring Integration 7.0 sample project combining an **Order Processing Pipeline** over HTTP and a **File Polling & Processing** pipeline, demonstrating Java DSL with content-based routing, publish-subscribe channels, and wire tap patterns.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for PostgreSQL)

## Database

Start PostgreSQL:

```bash
docker compose up -d
```

This starts PostgreSQL 16 on port 5432 with a persistent volume (`pgdata`).

## Build

```bash
mvn clean package
```

## Run

```bash
mvn spring-boot:run
```

Or build and run the container:

```bash
docker build -t order-flow .
docker run --rm -p 8080:8080 order-flow
```

The server starts on `http://localhost:8080`. The application auto-creates three directories at startup:

| Directory           | Purpose                                    |
|---------------------|--------------------------------------------|
| `input/`            | Drop `.csv` files here for file polling    |
| `output/success/`   | Archived order files for successful orders |
| `output/failed/`    | Error logs for failed orders               |

## Architecture

### HTTP Flow (`POST /api/orders`)

```
Client JSON → Http.inboundGateway
  → JsonToOrderTransformer (JSON → Order)
  → OrderValidationService (validate fields)
  → Router (total ≤ $100 ? EXPRESS : REVIEW)
    → EXPRESS: OrderProcessingService.processExpress
    → REVIEW:  OrderProcessingService.processReview
  → PublishSubscribeChannel (wire tap)
    → Subscribe 1: archiveChannel → archiveFlow (file writer)
    → Subscribe 2: transform Order → response String
```

### File Polling Flow (drops `*.csv` into `input/`)

```
File poller (5s delay) → FileToStringTransformer
  → CsvToOrderTransformer (CSV → Order, retry advice for transient handler failures)
  → OrderValidationService (validate fields)
  → Router (total ≤ $100 ? EXPRESS : REVIEW)
    → EXPRESS: OrderProcessingService.processExpress
    → REVIEW:  OrderProcessingService.processReview
  → archiveChannel → archiveFlow (file writer)
```

### Error Flow

Any exception during processing (validation failure, parse error, duplicate order IDs) is sent to an error flow that writes a failure log to `output/failed/` and returns a JSON `{"error":"..."}` response for HTTP requests.

## Project Structure

```
src/main/java/com/example/orderflow/
  OrderFlowApplication.java          -- @SpringBootApplication entry point
  config/
    IntegrationCommonConfig.java     -- shared channels, graph server, summary store, startup dirs
    HttpIntegrationConfig.java       -- HTTP order and error flows
    FileIntegrationConfig.java       -- file poller, archive, error, and summary flows
    OrderProperties.java             -- @ConfigurationProperties(prefix = "order")
  model/
    Order.java                       -- Order record (id, customer, description, total)
    OrderEntity.java                 -- JPA entity for PostgreSQL persistence
    OrderStatus.java                 -- PENDING / PROCESSED / REVIEWED / FAILED enum
  transformer/
    JsonToOrderTransformer.java      -- Jackson 3 JSON → Order
    CsvToOrderTransformer.java       -- CSV line → Order
  service/
    OrderValidationService.java      -- Validates id, customer, total
    OrderProcessingService.java      -- processExpress / processReview
    OrderStore.java                  -- JPA-backed order store
    OrderRepository.java             -- Spring Data JPA repository
  web/
    ErrorHandler.java                -- @RestControllerAdvice
    OrderNotFoundException.java      -- 404 exception
    IntegrationGraphController.java  -- /api/integration/graph

src/test/java/com/example/orderflow/
  OrderFlowIntegrationTest.java      -- E2E tests

src/main/resources/
  application.yml                    -- Server port, PostgreSQL, JPA, app name
```

## Usage Examples

### Submit an EXPRESS order (total ≤ $100)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"id":"ORD-001","customer":"Alice","description":"Book","total":25.00}'
```

Response: `Order ORD-001 submitted (EXPRESS)`

### Submit a REVIEW order (total > $100)

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"id":"ORD-002","customer":"Bob","description":"Laptop","total":1500.00}'
```

Response: `Order ORD-002 submitted (REVIEW)`

### Submit an invalid order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{"id":"","customer":"","total":-5}'
```

Response: HTTP 400, `{"error":"Order id must not be blank"}`

### Retrieve an order

```bash
curl http://localhost:8080/api/orders/ORD-001
```

Response: `{"id":"ORD-001","customer":"Alice","description":"Book","total":25.00,"status":"PROCESSED"}`

Returns 404 for unknown IDs.

### List orders

```bash
curl http://localhost:8080/api/orders
```

Response: JSON array of stored orders.

Requests and file messages now carry a `correlationId` header that is also pushed into MDC for service logs.

### File polling — drop a CSV

Create `input/order.csv`:

```csv
ORD-010,David,Keyboard,75.00
```

The poller picks it up within 5 seconds and writes `output/success/order-ORD-010.txt`.

## Tests

```bash
mvn test
```

Runs 39 tests (integration + unit):

| Test                          | Verifies                                          |
|-------------------------------|---------------------------------------------------|
| `httpExpressOrder`            | POST with total ≤ $100 → 200 + archived file      |
| `httpReviewOrder`             | POST with total > $100 → 200 + archived file      |
| `httpInvalidOrder`            | POST with bad data → 400 + error body             |
| `httpGetOrder_returnsStoredOrder` | POST then GET → 200 with order JSON           |
| `httpGetUnknownOrder_returns404`  | GET unknown ID → 404                          |
| `integrationGraph_returnsNodesAndLinks` | GET /api/integration/graph → 200        |
| `fileOrderPolled`             | CSV drop → processed output file within 15s      |
| `summaryFlow_releasesAfterTenOrders` | 10 summary messages → summary CSV file     |

## Dependencies

| Dependency                          | Scope  | Purpose                              |
|-------------------------------------|--------|--------------------------------------|
| spring-boot-starter-web             | main   | Embedded Tomcat, REST endpoints      |
| spring-boot-starter-integration     | main   | Spring Integration framework         |
| spring-integration-file             | main   | File inbound/outbound adapters       |
| spring-integration-http             | main   | HTTP inbound gateway                 |
| spring-boot-starter-data-jpa        | main   | JPA + Hibernate ORM                  |
| postgresql                          | runtime| PostgreSQL JDBC driver               |
| spring-boot-starter-actuator        | main   | Actuator endpoints                   |
| micrometer-registry-prometheus      | main   | Prometheus metrics                   |
| spring-boot-starter-test            | test   | JUnit 5, AssertJ                     |
| spring-boot-resttestclient          | test   | TestRestTemplate for integration tests|
| spring-boot-restclient              | test   | RestTemplateBuilder (required by resttestclient)|
| spring-integration-test             | test   | SI test utilities                    |
| h2                                  | test   | In-memory database for tests         |
| awaitility                          | test   | Async file polling assertions        |

## Key Configuration

The application uses **Spring Boot 4.1.0** with **Jackson 3** (`tools.jackson.databind.json.JsonMapper`), **Jakarta EE 11**, and **Java 21** records.

`order.routing.express-threshold` controls the routing cutoff for EXPRESS vs REVIEW handling.
