# Card Management API

Spring Boot REST API for creating cards under a card program and retrieving card details.

## Tech Stack

- Java
- Spring Boot
- Spring Security
- OAuth2 / JWT
- Dex
- H2 Database
- JPA / Hibernate
- Maven
- Swagger / OpenAPI

## APIs

### Create Card

POST /v1/cards

Required headers:

Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
X-Idempotency-Key: <unique-idempotency-key>
X-Channel: <channel>

The JWT `sub` claim is used as the partner ID.

The endpoint requires the `cards:write` scope.

### Card Details

POST /cards/details

Required headers:

Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>

Optional header:

X-Channel: MOBILE | WEB | BRANCH | PARTNER

The JWT `sub` claim is used as the partner ID.

The endpoint uses the authenticated partner identity for authorization and rate limiting.

#### Request

```json
{
  "cards": [
    {
      "cardId": "CARD001"
    }
  ],
  "customerId": "0012342"
}
```

Request rules:

- `cards` is mandatory.
- Between 1 and 10 cards can be requested.
- Duplicate card IDs are de-duplicated.
- `customerId` is conditional based on channel/entitlement.
- Each `cardId` must not exceed 20 characters.
- `customerId` must not exceed 20 characters.

#### Response

```json
{
  "referenceId": "REQ-001",
  "responseCode": "00",
  "responseDesc": "Card details retrieved successfully",
  "cards": [
    {
      "cardId": "CARD001",
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

#### Card Details Response Codes

- `00` - Card details retrieved successfully
- `10` - No card details found
- `90` - Customer-card ownership mismatch

The API supports partial success when some requested card IDs are found and others are unknown.

Card numbers are returned in masked form.

## Response Codes

HTTP Status | Response Code | Meaning
---|---|---
200 | 00 | Card created successfully / Card details retrieved successfully
200 | 10 | Business decline / No card details found
200 | 90 | Customer-card ownership mismatch
400 | 01 | Invalid request
401 | 98 | Unauthorized
403 | 98 | Forbidden
409 | 09 | Idempotency key conflict
429 | - | Rate limit exceeded

## Idempotency

The API supports idempotent card creation using the `X-Idempotency-Key` header.

- Same key + same request -> previous response is replayed.
- Same key + different request -> 409 Conflict with response code 09.
- Database uniqueness protects against duplicate idempotency keys.

The `X-Idempotency-Key` header is required for card creation.

It is not required for the Card Details API.

## Rate Limiting

Card creation and card details retrieval are rate limited per partner.

The partner is identified using the JWT `sub` claim.

Requests exceeding the configured rate limit receive:

HTTP 429 Too Many Requests

## Security

The API uses OAuth2 Resource Server with JWT authentication.

JWT authorities are read from the `groups` claim and converted using the `SCOPE_` prefix.

Example:

```json
{
  "sub": "partner-001",
  "groups": [
    "cards:write"
  ]
}
```

The authenticated partner identity is obtained from the JWT `sub` claim.

The Card Details API performs customer-card ownership validation when `customerId` is supplied in the request.

A token without the `cards:write` scope receives:

403 Forbidden

## Request ID

`X-Request-Id` is required for API requests.

The value is echoed in successful responses and handled application/security error responses.

The same value is also returned as `referenceId` in API response payloads where applicable.

## Card Details Validation

The Card Details API validates:

- Request body presence.
- `cards` list presence.
- At least one card must be supplied.
- Maximum of 10 cards per request.
- `cardId` must not be blank.
- `cardId` must not exceed 20 characters.
- `customerId` must not exceed 20 characters.

Duplicate card IDs in the same request are processed only once.

## Card Details Ownership

When a `customerId` is supplied, the API verifies that the requested card belongs to that customer.

If the card and customer ownership do not match, the API returns:

```json
{
  "referenceId": "REQ-005",
  "responseCode": "90",
  "responseDesc": "Customer-card ownership mismatch - details request declined",
  "cards": []
}
```

## Card Details Partial Success

The Card Details API supports partial success.

If multiple cards are requested and some card IDs are not found, the API returns the details of the cards that were successfully found.

For example:

```json
{
  "referenceId": "REQ-006",
  "responseCode": "00",
  "responseDesc": "Card details retrieved successfully",
  "cards": [
    {
      "cardId": "CARD001"
    }
  ]
}
```

## PAN Masking

Card numbers are returned in masked form.

Example:

```text
4111XXXXXXXX1234
```

The card creation flow stores the masked card number in the `Card` entity, and the Card Details API returns the stored masked value.

Full card numbers are not returned by the Card Details API.

## Local Development

Swagger, SQL logging, and the H2 console are enabled only through the `local` profile.

Start the application with:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

The local profile enables:

- Swagger/OpenAPI
- Swagger UI
- H2 Console
- SQL logging
- Hibernate formatted SQL

These settings are intended for local development only.

## Swagger / OpenAPI

Swagger/OpenAPI documentation is available when running with the `local` profile.

The APIs include OpenAPI annotations describing:

- API operation
- Successful response
- Validation errors
- Unauthorized responses
- Forbidden responses

## TLS / HTTPS Deployment

TLS/HTTPS should be terminated at the reverse proxy or API gateway in front of the Spring Boot application.

Recommended deployment flow:

```text
Client
  |
  | HTTPS
  v
Reverse Proxy / API Gateway
  |
  | HTTP or internal HTTPS
  v
Spring Boot Application
```

The application enables HTTP Strict Transport Security (HSTS):

- includeSubDomains=true
- maxAge=31536000

Production deployments should configure TLS certificates, private keys, supported TLS versions, and cipher suites at the reverse proxy/API gateway according to the organization's security standards.

Do not commit production private keys or certificates to the repository.

## Testing

Run all tests with:

```bash
mvn clean test
```

The project includes tests for:

### Card Creation

- Card creation
- Validation
- Invalid card programs
- Inactive card programs
- Card program type mismatch
- Idempotency replay
- Idempotency conflict
- Rate limiting
- Security scope enforcement
- X-Request-Id handling

### Card Details

- Card details retrieval
- Card details validation
- Card not found
- Duplicate card handling
- Partial success
- Customer-card ownership mismatch
- Customer ID handling
- Rate limiting
- X-Request-Id handling
- Internal repository exception handling

## Git Workflow

API changes are developed on feature branches and submitted through pull requests for review.

The `main` branch should not be directly modified without the appropriate review and approval.

## Notes

- Production secrets should not be committed to Git.
- Production TLS certificates and private keys should be managed outside the repository.
- Swagger and H2 Console are intended for local development.
- Card numbers must remain masked in API responses.
- API business outcomes should be determined using `responseCode` rather than HTTP 200 alone.
