# PEPE Trading Bot

Phase 1 scaffold for a manually triggered PEPE spot trading assistant.

## What is in this scaffold
- Spring Boot 3.3 app on Java 21
- Thymeleaf dashboard at `/dashboard`
- REST endpoints under `/api`
- H2-backed trade log storage
- Dockerfile for container builds

## Run locally
```bash
mvn spring-boot:run
```

Then open:
- `http://localhost:8080/dashboard`
- `http://localhost:8080/actuator/health`

## IntelliJ
Open the `pom.xml` in IntelliJ as a Maven project, then run `PepeTradingBotApplication`.
For local Secret Manager access, authenticate with Google Application Default Credentials and set `GCP_PROJECT_ID` or `GOOGLE_CLOUD_PROJECT`.

## Deployment
Terraform is kept in the repo for later, but deployment is deferred until after you review the app in IntelliJ.

## Status
Phase 1 scaffolding is in place, including Kraken wiring, UI hardening, and integration tests.

## Kraken verification
The integration suite includes optional live smoke tests for public price and private balance access. Set `RUN_LIVE_KRAKEN_TESTS=true` and authenticate to Google Cloud so the app can read the Kraken secrets from Secret Manager.
