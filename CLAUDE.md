# Lunch — Claude Context

## What this project does

Scrapes restaurant websites using per-site Playwright DOM scrapers to extract today's lunch menu, and publishes the result as a static GitHub Pages site. Runs automatically on weekdays via GitHub Actions. No LLM involved — each restaurant has a dedicated scraper class that targets that site's HTML structure directly.

## Key commands

```bash
# Build (skips tests, produces fat jar)
mvn package -DskipTests

# Install Playwright browser (required once after build, and after Playwright version upgrades)
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar com.microsoft.playwright.CLI install chromium

# Run all tests
mvn test -Dgroups="!docker"

# Generate lunch page (scrapes real URLs)
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.Main

# Serve the generated page locally at http://localhost:8080
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.DevServer

# Capture fresh HTML fixtures for all restaurants (run when a site redesigns)
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.FixtureCapture
```

## Source files

| File | Role |
|---|---|
| `Main.java` | Entry point. Loads `restaurants.yaml`, owns Playwright browser lifecycle, calls `ScraperRegistry`, writes `output/`. |
| `ScraperRegistry.java` | Maps restaurant names to scraper instances. Unknown names fall back to `DefaultMenuScraper`. |
| `BaseMenuScraper.java` | Abstract base — handles Playwright navigation with timeout fallback, provides `dishesOf()` and `textsOf()` helpers. |
| `DefaultMenuScraper.java` | Heuristic fallback scraper. Uses JS to detect Swedish day-name headings and group dishes. Falls back to all `<li>` or `<p>` items. |
| `GrandScraper.java` … `BistroCominhScraper.java` | One class per restaurant. Each extends `BaseMenuScraper` and targets that site's specific HTML structure. |
| `WeeklyMenu.java` | Record: either per-day (`Map<DayOfWeek, List<Dish>>`) or all-week (`List<Dish>`). `forDay(DayOfWeek)` handles both transparently. |
| `Dish.java` | Record: `description`. Thin wrapper for future extensibility (price, dietary flags, etc.). |
| `LunchResult.java` | Record: `restaurant`, `dishes`, `error`. `success()` returns `error == null`. |
| `PageGenerator.java` | Writes `output/index.html` + one page per location. Swedish locale date. HTML-escapes all content. |
| `DevServer.java` | `com.sun.net.httpserver` static file server on port 8080. Serves `output/`. Sets `Cache-Control: no-cache`. |
| `Restaurant.java` | Record: `name`, `url`, `location`. |
| `FixtureCapture.java` | Dev utility — fetches and saves rendered HTML from all live restaurant pages to `src/test/resources/__files/`. |

## Tests

| Test class | What it covers | Speed |
|---|---|---|
| `WeeklyMenuTest` | `WeeklyMenu` factory methods and `forDay()` logic | Fast |
| `BaseMenuScraperTest` | `dishesOf()` / `textsOf()` helpers | Fast |
| `DefaultMenuScraperTest` | Day-grouped extraction, all-week fallback, empty page | Fast |
| `PageGeneratorTest` | HTML generation, XSS escaping, error cards, multi-restaurant output | Fast |
| `LunchIntegrationTest` | Full pipeline: registry → scraper → page generator, WireMock HTML stubs | Fast |
| `GrandScraperTest` … `BistroCominhScraperTest` | One test per restaurant, verifies non-empty dishes from saved HTML fixture | Fast |

Run all tests: `mvn test -Dgroups="!docker"`

## Adding a restaurant

1. Add entry to `src/main/resources/restaurants.yaml` under the correct location.
2. Add the URL to `FixtureCapture.TARGETS` and run `FixtureCapture` to save the HTML fixture.
3. Create `XxxScraper.java` extending `BaseMenuScraper` — inspect the fixture HTML to find the right selectors.
4. Create `XxxScraperTest.java` extending `ScraperTestBase` — assert non-empty dishes from the fixture.
5. Register in `ScraperRegistry` with the exact restaurant name from the YAML.

If no specific scraper is needed, `DefaultMenuScraper` is used automatically — check if it produces sensible results before writing a custom one.

## GitHub Actions / Pages

- Workflow: `.github/workflows/lunch.yml` — cron `22 5 * * 1-5` (weekdays) + manual dispatch.
- Builds fat jar, installs Playwright Chromium, runs scraper, deploys `output/` to `gh-pages` branch via `peaceiris/actions-gh-pages@v4`.
- Pages setup: **Settings → Pages → Source → Deploy from branch → `gh-pages` / `/ (root)`**. Run the workflow once first to create the branch.

## Local dev loop

```bash
mvn package -DskipTests
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.Main
# open http://localhost:8080 in browser
java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.DevServer
# edit scraper → re-run Main → refresh browser
```

## Tech stack

- Java 17, Maven
- Playwright (Chromium) for scraping — handles JS-rendered sites
- Jackson (YAML config parsing)
- JUnit 5, WireMock 3.9.1 (tests)
