# Lunch — Claude Context

## What this project does

Scrapes restaurant websites, sends the text to a local LLM (Ollama) to extract today's lunch menu, and publishes the result as a static GitHub Pages site. Runs automatically on weekdays at 11:00 CEST via GitHub Actions.

## Key commands

```bash
# Build (skips tests, produces fat jar)
mvn package -DskipTests

# Run all tests (needs Docker for the Ollama tests)
mvn test

# Run only fast tests (no Docker required)
mvn test -Dgroups="!docker"

# Run only real-Ollama tests (starts Docker container automatically)
mvn test -Dgroups="docker"

# Generate lunch page using mock menus (no Ollama needed, still scrapes real URLs)
LUNCH_MOCK=true java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.Main

# Serve the generated page locally at http://localhost:8080
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.DevServer
```

## Source files

| File | Role |
|---|---|
| `Main.java` | Entry point. `RESTAURANTS` list is defined here. Reads `LUNCH_MOCK`, `OLLAMA_URL`, `OLLAMA_MODEL` env vars. |
| `LunchScraper.java` | Fetches URL with Jsoup, strips script/style/nav/footer, truncates to 6000 chars. |
| `MenuExtractor.java` | Interface implemented by both `LlmClient` and `MockLlmClient`. |
| `LlmClient.java` | Posts to Ollama `/api/generate`. Timeout 120s. |
| `MockLlmClient.java` | Returns hardcoded menus keyed by restaurant name (Grand, Yogi), random Swedish samples for others. |
| `PageGenerator.java` | Writes `output/index.html`. Swedish locale date. HTML-escapes all content. |
| `DevServer.java` | `com.sun.net.httpserver` static file server on port 8080. Serves `output/`. Sets `Cache-Control: no-cache`. |
| `Restaurant.java` | Record: `name`, `url`. |
| `LunchResult.java` | Record: `restaurant`, `menu`, `error`. `success()` returns `error == null`. |

## Tests

| Test class | What it covers | Speed |
|---|---|---|
| `PageGeneratorTest` | HTML generation, XSS escaping, error cards, multi-restaurant output | Fast |
| `LunchIntegrationTest` | Full pipeline with WireMock for both restaurant HTTP and Ollama API. Scraper noise-stripping. | Fast |
| `LlmClientDockerTest` | Real Ollama (`llama3.2:1b`) in Docker. Verifies menu extraction and "no menu found" behaviour. | Slow first run (~2 min model pull), ~45s after |

## Docker test details

The Docker tests use `ProcessBuilder` with the system `docker` command — **not Testcontainers** — because `docker-java 3.4.x` (used by Testcontainers) sends API version 1.32 which Docker 29.x rejects (requires ≥1.40). The test starts container `lunch-ollama-test` on port 11435, pulls the model, runs, then removes the container. If Docker is unavailable the tests are skipped via `assumeTrue`.

## Adding restaurants

Edit `Main.java`:
```java
private static final List<Restaurant> RESTAURANTS = List.of(
    new Restaurant("Grand", "https://www.brasseriegrand.se/lunch"),
    new Restaurant("Yogi",  "https://restaurangyogi.com/lunch")
    // add more here
);
```

For `MockLlmClient`, add a fixed menu to the `FIXED_MENUS` map to match by name, otherwise a random sample is returned.

## GitHub Actions / Pages

- Workflow: `.github/workflows/lunch.yml` — cron `0 9 * * 1-5` (11:00 CEST weekdays) + manual dispatch.
- Starts `ollama/ollama:latest` via Docker, pulls `llama3.2:1b`, builds fat jar, runs it, deploys `output/` to `gh-pages` branch via `peaceiris/actions-gh-pages@v4`.
- Pages setup: **Settings → Pages → Source → Deploy from branch → `gh-pages` / `/ (root)`**. Run the workflow once first to create the branch.

## Local dev loop

```bash
mvn package -DskipTests
LUNCH_MOCK=true java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.Main
# open http://localhost:8080 in browser
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.DevServer
# edit → re-run the generate command → refresh browser
```

## Tech stack

- Java 17, Maven
- Jsoup (scraping), Jackson (JSON), Java built-in `HttpClient` (Ollama API calls)
- JUnit 5, WireMock 3.9.1 (fast tests)
- Ollama `llama3.2:1b` (~1.3 GB) as the local LLM
