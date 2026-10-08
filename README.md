# Eagle Bank One API

Spring Boot 3 / Java 21 API for the Eagle Bank take-home. It implements the user, account and transaction operations in the assignment, plus password login that returns a JWT.

The contract is `openapi.yaml`. Creating a user and logging in are public. Every other endpoint requires `Authorization: Bearer <accessToken>`.

## Run

Java 21 and Maven 3.9+ are required.

```bash
mvn clean verify
mvn spring-boot:run
```

The API listens on `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui.html`. H2 stores data in `data/`. Stop the app before deleting that directory if you want a clean database. The default JWT signing secret is for local use only. Set one before sharing a running instance:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
mvn spring-boot:run
```

Passwords are stored with BCrypt. Tokens last one hour, are signed with HS256, and are rejected after the user is deleted.

## Try it

```bash
curl -s -X POST http://localhost:8080/v1/users \
  -H 'Content-Type: application/json' \
  -d '{"name":"Matt","address":{"line1":"1 Example Road","town":"Stockport","county":"Greater Manchester","postcode":"SK1 1AA"},"phoneNumber":"+447700900123","email":"matt@example.com","password":"ExamplePassword123!"}'

curl -s -X POST http://localhost:8080/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"matt@example.com","password":"ExamplePassword123!"}'
```

Use `accessToken` as `TOKEN` and the user `id` as `USER_ID`:

```bash
curl -s -H "Authorization: Bearer $TOKEN" "http://localhost:8080/v1/users/$USER_ID"
curl -s -X POST http://localhost:8080/v1/accounts \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"Personal account","accountType":"personal"}'
```

Use the returned `accountNumber` as `ACCOUNT`:

```bash
curl -s -X POST "http://localhost:8080/v1/accounts/$ACCOUNT/transactions" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":100.25,"currency":"GBP","type":"deposit","reference":"Initial deposit"}'
curl -s -X POST "http://localhost:8080/v1/accounts/$ACCOUNT/transactions" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":20.10,"currency":"GBP","type":"withdrawal"}'
```

The balance is then 80.15.

## Endpoints

| Method | Path | Success |
|---|---|---|
| POST | `/v1/users` | 201 |
| POST | `/v1/auth/login` | 200 |
| GET, PATCH, DELETE | `/v1/users/{userId}` | 200, 200, 204 |
| POST, GET | `/v1/accounts` | 201, 200 |
| GET, PATCH, DELETE | `/v1/accounts/{accountNumber}` | 200, 200, 204 |
| POST, GET | `/v1/accounts/{accountNumber}/transactions` | 201, 200 |
| GET | `/v1/accounts/{accountNumber}/transactions/{transactionId}` | 200 |

Transactions cannot be updated or deleted. Missing or invalid tokens return 401. A resource that exists but belongs to someone else returns 403. A missing resource returns 404, including a transaction that belongs to a different account. Invalid input returns 400 with `message` and `details`. Other errors return `message`. Deleting a user who still has an account returns 409. A duplicate email returns 409. A withdrawal that would go below zero, or a deposit that would push the balance above 10000.00, returns 422.

## Decisions

- Signup requires a `password` because the original schema has no credential and the brief asks for JWT login. The password is never returned.
- The PDF says `accountId`. The OpenAPI file says `accountNumber`. The API follows the OpenAPI file.
- The original transaction id pattern `^tan-[A-Za-z0-9]$` allows one character, while the example is `tan-123abc`. Issued ids use `tan-` plus 12 alphanumeric characters, and the spec in this repo allows one or more.
- Amounts are `BigDecimal` values from 0.00 to 10000.00 with at most two decimal places. Zero is allowed because the schema minimum is 0.00.
- New accounts start at 0.00 GBP with sort code `10-10-10` and type `personal`.
- PATCH updates only fields that are present. A supplied address replaces the whole address, so omitted `line2` or `line3` clears that line. Unknown JSON properties are rejected.
- Email is stored in lowercase and must be unique.
- Creating a transaction locks the account row until the balance update and the transaction row commit together, so two withdrawals cannot overdraw the account.
- Deleting an account keeps the row and its transactions so the account number is never reused, but the API treats that account as missing. The assignment does not require the balance to be zero. A user can be deleted once they have no active account.
- The original `format` values that contained regular expressions are expressed as `pattern` in `openapi.yaml`.

`EagleBankApiTest` runs the assignment scenarios against the real security filter chain and an in-memory database.

## Structure and SOLID principles

The project and Maven artifact are named `eagle-bank-one-api`. Author: `mattbateup`.

- `model`: all production request, response and record types, including address data, error details and JWT properties. The test-only `Session` record also lives in a top-level test `model` package.
- `user`, `account`, `transaction`: controllers, application services, persistence entities and repository interfaces grouped by business area.
- `auth`: credential verification and token issuance in separate services; the controller only handles HTTP delegation.
- `common`, `config`, `security`: shared bank rules, validation, configuration and security infrastructure.

Each controller handles HTTP concerns, each service handles its business operation, and each repository handles persistence (single responsibility). Constructor injection and narrow framework interfaces (`PasswordEncoder`, `JwtEncoder`, `Clock`, repositories) support replacing collaborators without changing callers (open/closed and dependency inversion). Repository implementations honor their interface contracts (substitution), and collaborators expose only the operations callers need (interface segregation). No extra service interfaces are introduced where there is only one implementation and no substitution requirement.

Type Javadoc includes `@author mattbateup`; business operations document their behavior. Monetary values remain `BigDecimal`, and transaction row locking is preserved.

The supplied `openapi.yaml` is retained byte-for-byte. It already includes the password/login additions and corrected identifier patterns described above; this refactor does not revert those earlier contract decisions.

## Verification

Verified with Java 21 and Maven 3.9.9: 49 tests passed, no failures or skipped tests, and the executable Spring Boot JAR packaged successfully. Javadoc generation passed with doclint enabled (missing-comment warnings excluded). The hosted test environment required Mockito to start as an explicit Java agent because dynamic agent attachment was unavailable; no tests were disabled.

The OpenAPI model test verifies property names for every top-level API schema plus nested address and error-detail models. The 48 integration tests cover endpoint status codes, authentication, ownership, validation, balances and concurrent withdrawals. These checks cover the supplied contract scenarios, rather than a complete OpenAPI conformance certification.
