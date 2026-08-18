# Card Management API

Spring Boot REST API for creating cards under a card program.

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

## API

### Create Card

POST /v1/cards

Required headers:

Authorization: Bearer <JWT>
X-Request-Id: <unique-request-id>
X-Idempotency-Key: <unique-idempotency-key>
X-Channel: <channel>

The JWT `sub` claim is used as the partner ID.

The endpoint requires the `cards:write` scope.

## Response Codes

HTTP Status | Response Code | Meaning
200 | 00 | Card created successfully
200 | 10 | Business decline
400 | 01 | Invalid request
401 | 98 | Unauthorized
403 | 98 | Forbidden
409 | 09 | Idempotency key conflict

## Idempotency

The API supports idempotent card creation using the `X-Idempotency-Key` header.

- Same key + same request -> previous response is replayed.
- Same key + different request -> 409 Conflict with response code 09.
- Database uniqueness protects against duplicate idempotency keys.

## Rate Limiting

Card creation is limited to:

100 requests per partner per minute

The partner is identified using the JWT `sub` claim.

Requests exceeding the rate limit receive:

HTTP 429 Too Many Requests

## Security

The API uses OAuth2 Resource Server with JWT authentication.

JWT authorities are read from the `groups` claim and converted using the `SCOPE_` prefix.

Example:

{
  "sub": "partner-001",
  "groups": [
    "cards:write"
  ]
}

A token without the `cards:write` scope receives:

403 Forbidden

## Request ID

`X-Request-Id` is echoed in successful responses and handled application/security error responses.

## Local Development

Swagger, SQL logging, and the H2 console are enabled only through the `local` profile.

Start the application with:

mvn spring-boot:run -Dspring-boot.run.profiles=local

The local profile enables:

- Swagger/OpenAPI
- Swagger UI
- H2 Console
- SQL logging
- Hibernate formatted SQL

These settings are intended for local development only.

## TLS / HTTPS Deployment

TLS/HTTPS should be terminated at the reverse proxy or API gateway in front of the Spring Boot application.

Recommended deployment flow:

Client
  |
  | HTTPS
  v
Reverse Proxy / API Gateway
  |
  | HTTP or internal HTTPS
  v
Spring Boot Application

The application enables HTTP Strict Transport Security (HSTS):

- includeSubDomains=true
- maxAge=31536000

Production deployments should configure TLS certificates, private keys, supported TLS versions, and cipher suites at the reverse proxy/API gateway according to the organization's security standards.

Do not commit production private keys or certificates to the repository.

## Testing

Run all tests with:

mvn clean test

The project includes tests for:

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

## Notes

- Production secrets should not be committed to Git.
- Production TLS certificates and private keys should be managed outside the repository.
- Swagger and H2 Console are intended for local development.
