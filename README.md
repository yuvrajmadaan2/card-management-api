# Card Management API

## Production TLS / HTTPS

This application is designed to run behind a production API gateway
or load balancer.

TLS/HTTPS termination is expected to be handled at the API gateway
or load balancer.

The Spring Boot service does not need to terminate public TLS itself
when it only accepts connections from the trusted internal VPC or
cluster network.

The production deployment must ensure that:

- Public API traffic uses HTTPS.
- TLS is terminated at the API gateway or load balancer.
- The Spring Boot service is not directly exposed to the public internet.
- Communication from the gateway/load balancer to the service occurs
  over the trusted internal network.
- HSTS is enabled in Spring Security.
- The API gateway/load balancer is responsible for presenting and
  managing the public TLS certificate.

## Local Development

For local development, run the application with the `local`
Spring profile:

```text
mvn spring-boot:run -Dspring-boot.run.profiles=local