package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class LunchIntegrationTest extends ScraperTestBase {

    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 20);

    @Test
    void defaultScraperExtractsDayGroupedMenu() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <h2>Måndag</h2>
                          <ul>
                            <li>Pasta Carbonara 105kr</li>
                            <li>Röd linssoppa 90kr</li>
                          </ul>
                        </body></html>
                        """)));

        var restaurant = new Restaurant("UnknownCafe", server.url("/lunch"), "Test");
        var scraper = ScraperRegistry.get(restaurant.name());

        WeeklyMenu menu = scraper.scrape(page, restaurant.url());
        var dishes = menu.forDay(DayOfWeek.MONDAY);

        assertFalse(dishes.isEmpty());
        assertTrue(dishes.stream().anyMatch(d -> d.description().contains("Pasta Carbonara")));
    }

    @Test
    void fullPipelineProducesHtmlPage(@TempDir Path outputDir) throws Exception {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <h2>Måndag</h2>
                          <ul><li>Pasta Carbonara 105kr</li></ul>
                        </body></html>
                        """)));

        var restaurant = new Restaurant("Test Bistro", server.url("/lunch"), "Test");
        var scraper = ScraperRegistry.get(restaurant.name());

        WeeklyMenu menu = scraper.scrape(page, restaurant.url());
        var dishes = menu.forDay(DayOfWeek.MONDAY);
        var result = new LunchResult(restaurant, dishes, null);

        String html = new PageGenerator().generate(List.of(result), MONDAY);
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), html);

        String readHtml = Files.readString(outputDir.resolve("index.html"));

        assertTrue(readHtml.contains("Test Bistro"), "Restaurant name missing");
        assertTrue(readHtml.contains("<li>Pasta Carbonara 105kr</li>"), "Menu item missing");
        assertFalse(readHtml.contains("class=\"error\""), "Should not show error state");
    }

    @Test
    void scrapingErrorShowsErrorCardInPage(@TempDir Path outputDir) throws Exception {
        server.stubFor(get("/lunch").willReturn(serverError()));

        var restaurant = new Restaurant("Broken Café", server.url("/lunch"), "Test");
        LunchResult result;
        try {
            var scraper = ScraperRegistry.get(restaurant.name());
            scraper.scrape(page, restaurant.url());
            result = new LunchResult(restaurant, List.of(), null);
        } catch (Exception e) {
            result = new LunchResult(restaurant, null, "HTTP error fetching URL");
        }

        String html = new PageGenerator().generate(List.of(result), MONDAY);
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), html);

        String readHtml = Files.readString(outputDir.resolve("index.html"));
        assertTrue(readHtml.contains("Broken Café"));
        assertTrue(readHtml.contains("class=\"error\""));
    }
}
