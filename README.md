# Card Management API

Spring Boot REST API implementing five Card Management APIs from the internship implementation guide. The APIs are organized around the Card resource and follow a layered Controller → Service → Repository architecture.

## APIs Implemented

| # | API | Endpoint | Purpose |
|---|---|---|---|
| 1 | Create Card | `POST /v1/cards` | Create a card under a card program |
| 2 | Card Details | `POST /v1/cards/details` | Retrieve details for one or more cards |
| 3 | Card Status | `POST /v1/cards/setStatus` | Update the lifecycle status of a card |
| 4 | Transaction Controls | `POST /v1/txnControls` | Retrieve transaction controls for card(s) |
| 5 | Transaction Control Update | `POST /v1/txnControls/set` | Update one transaction channel control |

The five APIs correspond to Spec APIs 1, 9, 3, 10 and 11 respectively.

---

## Tech Stack

- Java
- Spring Boot
- Spring Security
- OAuth2 Resource Server / JWT
- Dex
- H2 Database
- Spring Data JPA / Hibernate
- Maven
- Swagger / OpenAPI
- JUnit 5
- Mockito

---

# 1. Common API Conventions

## Base URL

```text
/v1
```

All five APIs use the versioned `/v1` endpoint pattern.

## Common Headers

The following headers are used across the APIs:

```text
Content-Type: application/json
Authorization: Bearer <access_token>
X-Request-Id: <unique-request-id>
```

Optional channel header:

```text
X-Channel: MOBILE | WEB | BRANCH | PARTNER
```

`X-Request-Id` is propagated through the request and returned in the response header. Where applicable, the same value is also represented as `referenceId` in the response body.

## Idempotency

Only card creation requires:

```text
X-Idempotency-Key: <unique-idempotency-key>
```

Rules:

- `POST /v1/cards` — required
- `POST /v1/cards/details` — not required
- `POST /v1/cards/setStatus` — not required
- `POST /v1/txnControls` — not required
- `POST /v1/txnControls/set` — not required

For card creation:

- Same idempotency key + same request → previous response is replayed.
- Same idempotency key + different request → idempotency conflict.
- Database uniqueness is used to protect idempotency records.

---

# 2. Authentication and Authorization

The application uses Spring Security OAuth2 Resource Server with JWT authentication.

The JWT `sub` claim is treated as the partner ID.

Example JWT:

```json
{
  "sub": "partner-001",
  "groups": [
    "cards:write"
  ]
}
```

JWT authorities are mapped from the `groups` claim using the application's `SCOPE_` authority convention.

Authentication and authorization are enforced before business processing.

Partner identity is also used for partner-level rate limiting.

---

# 3. Security Rules

The implementation follows these security requirements:

- Never return a full PAN.
- Never log PAN, CVV, PIN, tokens or other secrets.
- Card numbers are returned in masked form.
- H2 Console and Swagger/OpenAPI are intended for local development only.
- H2 access is protected outside the local development setup.
- CORS is restricted to configured origins rather than using a wildcard.
- HSTS is enabled.
- Production TLS termination should be handled by the reverse proxy/API gateway.
- Production certificates, private keys and secrets must not be committed to Git.

Example masked PAN:

```text
4111XXXXXXXX1234
```

---

# 4. API 1 — Create Card

## Endpoint

```http
POST /v1/cards
```

## Required Headers

```text
Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
X-Idempotency-Key: <unique-idempotency-key>
X-Channel: <channel>
```

## Request

```json
{
  "card": {
    "cardProgramType": "P",
    "cardType": "V",
    "cardProgramId": "PRGM001"
  }
}
```

## Main Request Rules

- `card` is mandatory.
- `cardProgramType` must use the supported program type values.
- `cardType` must use the supported card type values.
- `cardProgramId` is mandatory and length validated.
- The selected card program must be valid/active.
- Card program and card type compatibility is validated.
- Idempotency is enforced.

## Success Response

```json
{
  "cardNumber": "4111XXXXXXXX1234",
  "expiryDate": "07/2031",
  "cardId": "110195026979612",
  "referenceId": "f3a1c9e0-9f2b-4a7e-b6c1-2d9e8a4f7c10",
  "responseCode": "00",
  "responseDesc": "Card created successfully"
}
```

## Important Edge Cases

- Invalid card program ID
- Inactive card program
- Card program/card type mismatch
- Idempotency replay
- Idempotency conflict
- Validation failure
- Rate limiting

---

# 5. API 2 — Card Details

## Endpoint

```http
POST /v1/cards/details
```

## Required Headers

```text
Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
```

Optional:

```text
X-Channel: MOBILE | WEB | BRANCH | PARTNER
```

## Request

```json
{
  "cards": [
    {
      "cardId": "110195026979612"
    }
  ],
  "customerId": "0012342"
}
```

## Request Rules

- `cards` is mandatory.
- At least 1 card and at most 10 cards can be requested.
- Duplicate card IDs are de-duplicated.
- `cardId` cannot be blank and is length validated.
- `customerId` is conditional based on channel/entitlement rules.
- Ownership is validated when customer information is applicable.
- Partner identity is obtained from JWT `sub`.

## Success Response

```json
{
  "referenceId": "REQ-001",
  "responseCode": "00",
  "responseDesc": "Card details retrieved successfully",
  "cards": [
    {
      "cardId": "110195026979612",
      "cardProgramType": "P",
      "cardType": "V",
      "cardProgramId": "PRGM001",
      "cardProgramName": "WizzPlus Multicurrency Prepaid",
      "cardNumber": "4111XXXXXXXX1234",
      "expiryDate": "07/2031",
      "cardStatus": "A",
      "cardStatusDesc": "ACTIVE",
      "nameOnCard": "Chuck Yeager",
      "customerId": "0012342",
      "issuedDate": "2026-07-07"
    }
  ]
}
```

## Partial Success

If multiple cards are requested and only some are found, the API returns the details of the cards that were successfully resolved instead of failing the complete request.

## Ownership Mismatch

```json
{
  "referenceId": "REQ-005",
  "responseCode": "90",
  "responseDesc": "Customer-card ownership mismatch - details request declined"
}
```

## Response Codes

```text
00 - Card details retrieved successfully
10 - No card details found
90 - Customer-card ownership mismatch
```

---

# 6. API 3 — Update Card Status

## Endpoint

```http
POST /v1/cards/setStatus
```

## Required Headers

```text
Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
```

Optional:

```text
X-Channel: MOBILE | WEB | BRANCH | PARTNER
```

## Request

```json
{
  "card": {
    "cardId": "110195026979612",
    "statusCode": "S",
    "reasonCode": "CUSTREQ",
    "remarks": "Temporary travel freeze"
  }
}
```

## Request Fields

- `card.cardId`
- `card.statusCode`
- `card.reasonCode`
- `card.remarks`

Supported status values are:

```text
A - Active
B - Blocked
S - Suspended
R - Replaced
I - Inactive
```

## Validation

- `card` is mandatory.
- `cardId` is mandatory.
- `statusCode` is mandatory and validated.
- `reasonCode` is mandatory for `B`, `S` and `R`.
- `remarks` is optional.
- Status transitions are validated against the applicable specification rules.
- Terminal status restrictions are enforced.
- Reason-code applicability is validated against the status rules defined by the specification.

## Success Response

```json
{
  "cardNumber": "4111XXXXXXXX1234",
  "cardProgramName": "WizzPlus Multicurrency Prepaid",
  "customerId": "0012342",
  "responseCode": "00",
  "responseDesc": "Card status updated to TEMP SUSPENDED"
}
```

## Business Validation Example

```json
{
  "responseCode": "36",
  "responseDesc": "Reason code not applicable for requested status"
}
```

## Edge Cases

- Invalid status
- Invalid status transition
- Terminal status mutation
- Missing reason code for `B`, `S` or `R`
- Reason code not applicable to requested status
- Card not found
- Card already in requested status
- Unauthorized status mutation
- Repository/system failure

---

# 7. API 4 — Read Transaction Controls

## Endpoint

```http
POST /v1/txnControls
```

## Required Headers

```text
Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
```

Optional:

```text
X-Channel: MOBILE | WEB | BRANCH | PARTNER
```

## Request

```json
{
  "cards": [
    {
      "cardId": "110195026979612"
    }
  ],
  "customerId": "0012342"
}
```

## Request Rules

- `cards` is mandatory.
- Between 1 and 10 cards can be requested.
- Each `cardId` is validated.
- `customerId` is conditional based on the applicable channel/entitlement rules.
- Ownership checks are consistent with Card Details.
- Unknown card IDs are handled according to the API's partial/not-found behavior.

## Seven Supported Channels

Every returned card has a normalized transaction-control matrix containing:

```text
ATM
POS
ECOM
NFC
MAG
DOM
INT
```

Each channel contains:

```json
{
  "channelType": "ATM",
  "allowed": true,
  "editable": true
}
```

## Sample Response

```json
{
  "responseCode": "00",
  "responseDesc": "Transaction channel controls retrieved successfully",
  "channels": [
    {
      "cardId": "110195026979612",
      "lists": [
        {
          "channelType": "ATM",
          "allowed": true,
          "editable": true
        },
        {
          "channelType": "POS",
          "allowed": true,
          "editable": true
        },
        {
          "channelType": "ECOM",
          "allowed": true,
          "editable": true
        },
        {
          "channelType": "NFC",
          "allowed": true,
          "editable": true
        },
        {
          "channelType": "MAG",
          "allowed": false,
          "editable": true
        },
        {
          "channelType": "DOM",
          "allowed": false,
          "editable": false
        },
        {
          "channelType": "INT",
          "allowed": true,
          "editable": true
        }
      ]
    }
  ]
}
```

## Important Behavior

Transaction controls are represented as the effective state for the card.

Where a persisted transaction-control value exists, the effective returned value should reflect that persisted state. Otherwise, applicable program/card-status defaults are used.

## Edge Cases

- Unknown card
- Partial card lookup
- Ownership mismatch
- Missing customer ID where required
- Blocked/replaced card behavior
- Program defaults for cards that are not yet issued, where applicable

---

# 8. API 5 — Update Transaction Control

## Endpoint

```http
POST /v1/txnControls/set
```

## Required Headers

```text
Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
```

Optional:

```text
X-Channel: MOBILE | WEB | BRANCH | PARTNER
```

## Request

```json
{
  "cardId": "110195026979612",
  "customerId": "0012342",
  "channel": {
    "channelType": "ATM",
    "allowed": false
  }
}
```

## Supported Channel Types

```text
ATM
POS
ECOM
NFC
MAG
DOM
INT
```

## Rules

- Only one transaction-control change is made per request.
- `cardId` is mandatory.
- `channel` is mandatory.
- `channel.channelType` must be one of the supported channel values.
- `channel.allowed` is mandatory.
- Ownership/entitlement checks are enforced where applicable.
- Blocked/replaced/inactive card restrictions are enforced.
- Non-editable controls cannot be changed.
- Updates are performed transactionally.
- The effective persisted state is returned.
- If the requested value is already the current value, the operation is treated as a no-op.

## Success Response

```json
{
  "cardId": "110195026979612",
  "responseCode": "00",
  "responseDesc": "Channel control updated successfully",
  "channel": {
    "channelType": "ATM",
    "allowed": false,
    "editable": true
  }
}
```

## Locked Control Example

```json
{
  "responseCode": "60",
  "responseDesc": "Control locked by program policy — change not permitted"
}
```

## Edge Cases

- Card not issued/activated
- Card blocked
- Card replaced
- Non-editable control
- Invalid channel type
- Ownership/entitlement failure
- Same requested value as current value
- Repository/system failure
- Risk/step-up requirements where applicable

---

# 9. Common Error Model

The application uses a consistent business error structure:

```json
{
  "referenceId": "REQ-001",
  "responseCode": "01",
  "responseDesc": "Invalid request"
}
```

The standard fields are:

- `referenceId`
- `responseCode`
- `responseDesc`

HTTP status and business `responseCode` are separate concepts. A response with HTTP 200 must still be evaluated using `responseCode`.

## HTTP Baseline

| HTTP Status | Meaning |
|---|---|
| 200 | Successful response or business decline |
| 400 | Invalid/missing request data |
| 401 | Invalid or missing authentication |
| 403 | Scope, ownership or entitlement failure |
| 404 | Endpoint/resource not found |
| 409 | Idempotency conflict where applicable |
| 422 | Semantic/business validation failure |
| 429 | Rate limit exceeded |
| 500 | Internal system error |
| 504 | Downstream timeout where applicable |

---

# 10. Rate Limiting

Requests are rate limited per partner.

The partner is identified from:

```text
JWT sub claim
```

The configured exercise limit is:

```text
100 requests per partner
```

Requests exceeding the configured limit receive:

```text
HTTP 429 Too Many Requests
```

The response also preserves the request ID where applicable.

---

# 11. Layered Architecture

The project follows:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Database
```

## Controller

Responsible for:

- Request mapping
- Request validation
- Header extraction
- Authentication context
- Rate-limit check
- Calling the service
- Mapping the service response
- Returning `X-Request-Id`

Controllers do not contain repository/business logic.

## Service

Responsible for:

- Business rules
- Ownership checks
- Validation beyond simple DTO validation
- Card status rules
- Transaction-control rules
- Idempotency orchestration
- Transaction boundaries

## Repository

Responsible for:

- Database access
- Reusing existing queries where possible
- Minimal custom queries when required

## DTOs

DTOs represent the external API contract and are kept separate from persistence entities.

---

# 12. Logging

Application logging includes, where appropriate:

- API name
- Request/reference ID
- Card ID or other non-secret identifiers
- Partner ID where useful
- Channel
- Execution time

Sensitive information must never be logged, including:

- Full PAN
- CVV
- PIN
- Access tokens
- Session assertions
- Other secrets

---

# 13. OpenAPI / Swagger

OpenAPI annotations are included for the APIs.

Swagger/OpenAPI is enabled for local development through the `local` profile.

Run:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Local development enables:

- Swagger/OpenAPI
- Swagger UI
- H2 Console
- SQL logging
- Hibernate formatted SQL

These settings are intended for local development only.

---

# 14. Database

The application uses:

- H2
- Spring Data JPA
- Hibernate

The main resource is the Card entity.

Transaction-control updates are persisted so that the effective state can be returned by the transaction-control read API.

---

# 15. PAN Masking

PAN/card numbers are always returned in masked form.

Example:

```text
4111XXXXXXXX1234
```

The full PAN must not be exposed in:

- API responses
- logs
- error messages
- test output

The Card Details API returns the masked card number stored by the card creation flow.

---

# 16. Testing

Run all tests with:

```bash
mvn clean test
```

The project contains controller and service tests for the implemented APIs.

## API 1 Tests

Coverage includes:

- Successful card creation
- Request validation
- Invalid card program
- Inactive card program
- Card program/card type mismatch
- Idempotency replay
- Idempotency conflict
- Rate limiting
- Security scope behavior
- Request ID handling

## API 2 Tests

Coverage includes:

- Successful card-details retrieval
- Request validation
- Card not found
- Duplicate card handling
- Partial success
- Customer-card ownership mismatch
- Customer ID handling
- Rate limiting
- Request ID handling
- Repository/internal exception handling

## API 3 Tests

Coverage includes:

- Successful status update
- Validation failures
- Missing reason code
- Invalid status
- Status transition validation
- Terminal-state protection
- Reason-code applicability
- Same-status/no-op behavior
- Card not found
- Internal exception handling

## API 4 Tests

Coverage includes:

- Successful seven-channel response
- Card validation
- Card not found/partial lookup behavior
- Ownership validation
- Customer ID handling
- Status-specific behavior
- Persisted transaction-control override behavior
- Rate limiting
- Request ID handling
- Internal exception handling

## API 5 Tests

Coverage includes:

- Successful control update
- Invalid channel type
- Ownership validation
- Card not found
- Blocked/replaced/inactive card behavior
- Non-editable control
- No-op update
- Effective post-update response
- Repository/internal exception handling

---

# 17. Local Development

Clone the repository and switch to the branch you want to work on.

Install/build:

```bash
mvn clean install
```

Run locally:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

Run tests:

```bash
mvn clean test
```

---

# 18. Git Branch Structure

The APIs are developed on separate feature branches.

Expected branches:

```text
main
api-2-card-details
api-3-card-status
api-4-transaction-controls
api-5-transaction-controls-set
```

The first API is maintained on the approved/main implementation, while subsequent APIs are developed on their respective feature branches.

Changes should be reviewed before merging into the main branch.

---

# 19. Internship Requirements Checklist

The implementation follows the internship guide requirements:

- [x] Controller/service/repository layering
- [x] Request/response DTOs
- [x] Input validation
- [x] Error handling
- [x] Unit tests
- [x] OpenAPI annotations
- [x] PAN masking
- [x] Request ID handling
- [x] JWT authentication
- [x] Partner ID from JWT `sub`
- [x] Per-partner rate limiting
- [x] Card creation idempotency
- [x] Card details partial success
- [x] Customer-card ownership validation
- [x] Card status update flow
- [x] Transaction-control seven-channel model
- [x] Transaction-control update flow
- [x] Control lock/editability enforcement
- [x] Transaction-control no-op behavior
- [x] Effective persisted transaction-control state
- [x] Local-only Swagger/H2 development setup
- [x] CORS restrictions
- [x] HSTS configuration

---

# 20. Definition of Done

An API is considered complete when:

1. API behavior matches the applicable specification.
2. Request and response contracts are implemented correctly.
3. Validation and business rules are enforced server-side.
4. Authentication and authorization are enforced.
5. Ownership/entitlement checks are implemented where required.
6. Sensitive information is never exposed.
7. Rate limiting is applied.
8. `X-Request-Id` is propagated.
9. Business response codes are handled correctly.
10. Unit/controller tests cover success and important failure paths.
11. OpenAPI documentation is present.
12. Mentor review comments are resolved.
13. `mvn clean test` passes.

---

# 21. Important Notes

- Do not commit production secrets.
- Do not commit production TLS private keys or certificates.
- Do not expose full PAN values.
- Do not log sensitive authentication or card information.
- Swagger and H2 Console are intended for local development.
- Check `responseCode` in addition to the HTTP status.
- Keep controllers free of repository/business logic.
- Reuse existing repositories and add only necessary queries.
- Keep API contracts aligned with the internship specification.