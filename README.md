# QurilishNazorat — REST Assured API Test Suite

> Automated API test suite for the **Novostroyka** public API — a Tashkent City Hokimiyat construction catalogue.
> Built with Java · REST Assured · JUnit 5 · AssertJ · Allure.

---

## 📋 Table of Contents

- [Project Overview](#project-overview)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Setup After Cloning](#setup-after-cloning)
- [Running the Tests](#running-the-tests)
- [Allure Report](#allure-report)
- [CI/CD Pipeline](#cicd-pipeline)
- [Test Coverage](#test-coverage)
- [Framework Architecture](#framework-architecture)
- [Configuration Reference](#configuration-reference)

---

## Project Overview

This project is a **REST API automation test suite** for the Novostroyka backend API (`https://uynazorati-dev-api.tashkent.uz`). All tested endpoints are **public** — no authentication is required.

The suite covers:
- Functional correctness (happy path, required fields, data types)
- Data integrity constraints (e.g. sub-counts summing to totals)
- Boundary and edge cases (min/max param values, query thresholds)
- Negative scenarios (missing required params, malformed IDs, out-of-range values)
- HTTP caching behaviour (ETag / `If-None-Match` → 304)
- Response header contracts (rate-limit headers on every response)
- Cursor-based pagination correctness

---

## Tech Stack

| Tool | Version | Purpose |
|---|---|---|
| Java | 25 (target 21) | Language |
| Spring Boot | 4.1.1 | Parent POM / build toolchain |
| REST Assured | 5.5.5 | HTTP client & response assertions |
| JUnit 5 (Jupiter) | 5.12.2 | Test runner, `@Nested`, `@ParameterizedTest` |
| AssertJ | 3.27.3 | Fluent Java assertions |
| Jackson | 2.19.1 | JSON serialisation support |
| Allure | 2.29.1 | HTML test report generation |
| java-dotenv | — | `.env` file support for local secrets |
| Maven Surefire | 3.5.3 | Test execution plugin |

---

## Project Structure

```
src/test/
├── java/com/jk/qurilishnazorat/
│   ├── base/
│   │   └── BaseTest.java           ← Abstract base; builds specs once via @BeforeAll
│   ├── common/
│   │   └── ApiConstants.java       ← All shared constants, headers, IDs, timeouts
│   ├── config/
│   │   └── ConfigReader.java       ← Loads config from .env → env vars → application.properties
│   ├── spec/
│   │   └── SpecFactory.java        ← Factory methods for RequestSpec and ResponseSpec
│   └── rest/
│       ├── SearchTests.java
│       ├── NotificationTests.java
│       ├── YaqinQurilishlarTests.java
│       ├── DeveloperTests.java
│       └── DistrictSummaryTests.java
└── resources/
    └── application.properties      ← Default config (base URL, API version path)
```

---

## Setup After Cloning

### 1. Prerequisites

- **JDK 21+** installed and `JAVA_HOME` set
- **Maven** available (`mvn -v`) or use the included `./mvnw` wrapper

### 2. Configure the Base URL

The test suite reads its base URL from three sources in priority order:

```
1. BASE_URL  environment variable       (highest — used in CI)
2. BASE_URL  key in .env file           (local development)
3. api.base.url in application.properties (fallback default)
```

**For local development — create a `.env` file** in the project root:

```bash
# .env  (never commit this file — add it to .gitignore)
BASE_URL=https://uynazorati-dev-api.tashkent.uz
```

> [!IMPORTANT]
> The `application.properties` file ships with `api.base.url=http://localhost:8080` as a safe default.
> **After cloning, set your real target URL** either in `.env` (local) or as a `BASE_URL` environment variable (CI/CD).

**For CI/CD pipelines** — just export the environment variable:

```bash
export BASE_URL=https://uynazorati-dev-api.tashkent.uz
mvn test
```

**Override at runtime without changing any file:**

```bash
mvn test -DBASE_URL=https://uynazorati-dev-api.tashkent.uz
```

### 3. Update Test Data IDs

Open [`ApiConstants.java`](src/test/java/com/jk/qurilishnazorat/common/ApiConstants.java) and verify the seeded IDs match your target database:

```java
public static final String KNOWN_PROJECT_ID   = "f5d9758e-7234-3ad0-8e37-1bc4030a2015";
public static final String KNOWN_DEVELOPER_ID = "cb7f1f24-b939-3b02-a0de-dc46e7481a38";
public static final String KNOWN_SALES_OFFICE_ID = "b245f3e7-f945-4653-b470-220d0cc7c6c6";
public static final int    KNOWN_DISTRICT_ID  = 101;
```

> [!NOTE]
> `DeveloperTests` fetches a real ID dynamically from the list endpoint via `@BeforeAll` — it does not rely on the hardcoded `KNOWN_DEVELOPER_ID` for most tests. Other test classes that need a guaranteed-valid ID use the constants above.

---

## Running the Tests

```bash
# Run the full suite
./mvnw test

# Run a single test class
./mvnw test -Dtest=SearchTests

# Run a specific nested class
./mvnw test -Dtest="SearchTests\$BoundaryTests"

# Run with a custom base URL
./mvnw test -DBASE_URL=https://uynazorati-dev-api.tashkent.uz

# Run and generate Allure results in one step
./mvnw test
```

---

## Allure Report

Allure results are written to `target/allure-results/` automatically when tests run.

### Generate and view the report

```bash
# Step 1 — Run the tests (produces target/allure-results/)
./mvnw test

# Step 2 — Build the static HTML report
./mvnw allure:report

# Step 3 — Open the report in a local browser (serves on http://localhost:8080)
./mvnw allure:serve
```

> [!TIP]
> `allure:serve` combines report generation and browser launch in one command — you can skip `allure:report` and go straight to `allure:serve` after running tests.

The report is also written to `target/site/allure-maven-plugin/` and can be deployed as a static site to any web server or CI artifact storage.

### What the report shows

- ✅ Test results by **Feature** → **Story** (matching `@Feature` and `@Story` annotations)
- 📎 Full **request & response** attached to every test step (via `AllureRestAssured` filter)
- ⏱ Response times and SLA breach indicators
- 📊 Pass / fail / broken breakdown per class and nested group
- 🔁 Parameterized test iterations with individual pass/fail per value

---

## CI/CD Pipeline

The pipeline is defined in [`.github/workflows/api-tests.yml`](.github/workflows/api-tests.yml) and runs on every push and pull request to `main`, `master`, and `develop`.

### Pipeline overview

```
push / PR / manual trigger
        │
        ▼
┌─────────────────┐
│  1. compile     │  GitHub cloud (ubuntu-latest)
│                 │  mvn test-compile — fast gate,
│                 │  catches errors before touching the real API
└────────┬────────┘
         │ passes
         ▼
┌─────────────────────────┐
│  2. test                │  ← MUST run on a self-hosted runner
│  [self-hosted,          │    located in Uzbekistan
│   uzbekistan]           │
│                         │  Steps:
│  • health check         │  curl /districts/summary with retries
│  • mvn test             │  runs 109 REST Assured tests
│  • upload artifacts     │  allure-results + surefire-reports
└──────────┬──────────────┘
           │ always (pass or fail)
           ▼
┌─────────────────┐
│  3. report      │  GitHub cloud (ubuntu-latest)
│                 │  mvn allure:report → HTML
│                 │  → GitHub Pages (main/master only)
│                 │  → downloadable artifact (all branches)
└─────────────────┘
```

### Why a self-hosted runner?

The target API (`uynazorati-dev-api.tashkent.uz`) enforces **geo-IP blocking** — requests from non-Uzbekistan IPs receive `403 Forbidden`. GitHub's hosted runners are US-based, so the test job must run on a machine with a Uzbekistan IP.

> [!IMPORTANT]
> The `compile` and `report` jobs run on GitHub's cloud — only the `test` job requires the self-hosted runner.

### Self-hosted runner setup (one-time)

1. Go to your repo → **Settings → Actions → Runners → New self-hosted runner**
2. Choose **Linux**, follow the download and configure steps shown
3. When prompted for labels, enter: `uzbekistan`
4. Install and start as a background service:

```bash
sudo ./svc.sh install
sudo ./svc.sh start
```

5. Verify it shows **Idle** (green) at **Settings → Actions → Runners**

### Required GitHub Secret

Go to **Settings → Secrets and variables → Actions → New repository secret**:

| Secret name | Value |
|---|---|
| `BASE_URL` | `https://uynazorati-dev-api.tashkent.uz` |

### GitHub Pages (live Allure report)

Go to **Settings → Pages** → source: **Deploy from a branch** → branch: `gh-pages` → folder: `/` → Save.

After the first successful run on `main` or `master`, the Allure report will be live at:
```
https://<your-username>.github.io/<repo-name>/allure-report/
```

### Manual trigger with a different URL

Go to **Actions → API Test Suite → Run workflow** → enter a custom base URL to run against a different environment without touching secrets.

### Health check — no more 502 flooding

Before any test runs, the CI pings `GET /api/v1/districts/summary` with retries (every 10s, up to 2 minutes). If the server is down it fails immediately with a clear message:

```
ERROR: API did not become healthy after 120s.
Last HTTP status: 502 — backend (nginx upstream) appears to be down.
```

This replaces what would otherwise be 109 confusing assertion failures all caused by the same root issue.

### Checking runner status

```bash
# From the ~/actions-runner directory on your self-hosted machine
sudo ./svc.sh status

# Or via systemd
sudo systemctl status actions.runner.<org>-<repo>.<runner-name>.service
```

---

## Test Coverage

**Total: 109 test cases** across 5 test classes.

### [`SearchTests`](src/test/java/com/jk/qurilishnazorat/rest/SearchTests.java) — `GET /search/suggest`

| Nested class | Tests | What's covered |
|---|---|---|
| `HappyPathTests` | 5 | 200 with items, required fields, type enum values, missing `q`, Cyrillic variants |
| `BoundaryTests` | 5 | Empty/single/2-char `q`, 120-char truncation, max 10 results |
| `CachingTests` | 2 | ETag present, `If-None-Match` → 304 |
| `HeaderTests` | 1 | All 3 `X-RateLimit-*` headers |

### [`NotificationTests`](src/test/java/com/jk/qurilishnazorat/rest/NotificationTests.java) — `GET /notifications`

| Nested class | Tests | What's covered |
|---|---|---|
| `HappyPathTests` | 4 | 200 with valid body, required fields, ISO-8601 timestamps, DESC ordering |
| `PaginationTests` | 6 | Default/custom/max size, invalid size → 400, cursor consistency, next-page, malformed cursor |
| `CachingTests` | 2 | ETag present, 304 |
| `HeaderTests` | 1 | Rate-limit headers |

### [`YaqinQurilishlarTests`](src/test/java/com/jk/qurilishnazorat/rest/YaqinQurilishlarTests.java) — `GET /geo/nearby`

| Nested class | Tests | What's covered |
|---|---|---|
| `HappyPathTests` | 6 | 200 with body, required fields, distance sort ASC, issueLevel filter, legalStatus filter, combined filters |
| `BoundaryTests` | 3 | Edge limit values, edge radiusM values, hasMore consistency |
| `NegativeTests` | 7 | Missing lat, missing lng, missing both, out-of-range coordinates ×4, invalid radiusM ×4, invalid limit ×4, invalid issueLevel, invalid legalStatus |
| `HeaderTests` | 3 | ETag, 304, rate-limit headers |

### [`DeveloperTests`](src/test/java/com/jk/qurilishnazorat/rest/DeveloperTests.java) — `GET /developers` & `GET /developers/{id}`

| Nested class | Tests | What's covered |
|---|---|---|
| `ListTests > HappyPathTests` | 3 | 200 with body, required fields, stats fields all >= 0 |
| `ListTests > SearchTests` | 4 | Valid `q`, single-char → empty, empty `q` → empty, 2-char accepted |
| `ListTests > PaginationTests` | 6 | Default/custom/max size, invalid size → 400, cursor consistency, next-page, malformed cursor |
| `ListTests > CachingTests` | 2 | ETag, 304 |
| `ListTests > HeaderTests` | 1 | Rate-limit headers |
| `DetailTests > HappyPathTests` | 4 | 200, required fields (id/name/legalName/tin/verificationSource/stats), stats >= 0, returned id matches requested |
| `DetailTests > NegativeTests` | 2 | Non-existent UUID → 404, malformed UUID → 400 |
| `DetailTests > CachingTests` | 2 | ETag, 304 |

### [`DistrictSummaryTests`](src/test/java/com/jk/qurilishnazorat/rest/DistrictSummaryTests.java) — `GET /districts/summary`

| Nested class | Tests | What's covered |
|---|---|---|
| `HappyPathTests` | 4 | 200 with non-empty items, required fields, all counts >= 0, no pagination fields |
| `DataIntegrityTests` | 3 | **Sub-counts sum to totalCount** for every district, unique IDs, unique non-blank names |
| `CachingTests` | 2 | ETag, 304 |
| `HeaderTests` | 1 | Rate-limit headers |

---

## Framework Architecture

```
ConfigReader
  │  reads .env / env vars / application.properties
  ▼
SpecFactory.requestSpec()          SpecFactory.responseSpec()
  │  base URI, base path            │  Content-Type: application/json
  │  Accept: application/json       │  response time < 5 000 ms
  │  timeouts (10s connect / 30s)   │
  │  AllureRestAssured filter        │
  ▼                                 ▼
BaseTest (@BeforeAll)
  │  requestSpec = SpecFactory.requestSpec()
  │  responseSpec = SpecFactory.responseSpec()
  │  RestAssured.baseURI / basePath = ConfigReader values
  ▼
Test classes extend BaseTest
  │  given(requestSpec)...when()...then().spec(responseSpec)
  └─ @Nested inner classes group tests by concern
```

### Key design decisions

| Decision | Rationale |
|---|---|
| `ConfigReader` with `.env` → env var → properties priority | `.env` for local dev secrets, env vars for CI/CD, properties as safe committed default |
| `SpecFactory` as a static factory | Keeps spec construction separate from when it's used — `BaseTest` owns the lifecycle |
| `protected static` fields in `BaseTest` | Built once per class, inherited by all `@Nested` classes without boilerplate |
| No `responseSpec` on error responses | Error responses return `application/problem+json`, not `application/json` — using `responseSpec` would fail the content-type assertion |
| Dynamic ID fetch in `DeveloperTests` | `@BeforeAll` grabs a real live ID from the list endpoint — tests stay green even when seed data changes |
| RFC 9457 structure assertions on 400/404 | Asserting `status` and `type` fields rather than exact error message strings — language-agnostic and refactor-safe |

---

## Configuration Reference

### `src/test/resources/application.properties`

```properties
api.base.url=http://localhost:8080    # overridden by .env or BASE_URL env var
api.version.url=/api/v1
```

### `.env` (create locally, do NOT commit)

```env
BASE_URL=https://uynazorati-dev-api.tashkent.uz
```

### Runtime overrides

```bash
# Override base URL
./mvnw test -DBASE_URL=https://other-env-api.tashkent.uz

# Run only a specific test class
./mvnw test -Dtest=DeveloperTests

# Run only a specific nested group
./mvnw test -Dtest="DeveloperTests\$DetailTests"
```
