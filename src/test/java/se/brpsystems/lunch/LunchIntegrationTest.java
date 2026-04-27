package se.brpsystems.lunch;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

class LunchIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 20);

    private WireMockServer restaurantServer;
    private WireMockServer ollamaServer;
    private LunchScraper scraper;

    @BeforeEach
    void setUp() {
        restaurantServer = new WireMockServer(wireMockConfig().dynamicPort());
        ollamaServer = new WireMockServer(wireMockConfig().dynamicPort());
        restaurantServer.start();
        ollamaServer.start();
        scraper = new LunchScraper();
    }

    @AfterEach
    void tearDown() {
        scraper.close();
        restaurantServer.stop();
        ollamaServer.stop();
    }

    @Test
    void fullPipelineProducesPageWithExtractedMenu(@TempDir Path outputDir) throws Exception {
        restaurantServer.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <h1>Veckans lunch</h1>
                          <p>Måndag: Pasta Carbonara 105kr</p>
                          <p>Vegetariskt: Röd linssoppa 90kr</p>
                        </body></html>
                        """)));

        ollamaServer.stubFor(post("/api/generate").willReturn(ok()
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"response":"Pasta Carbonara - 105kr\\nRöd linssoppa (vegetarisk) - 90kr"}
                        """)));

        var restaurant = new Restaurant(
                "Test Bistro",
                "http://localhost:" + restaurantServer.port() + "/lunch",
                null
        );

        var llm = new LlmClient("http://localhost:" + ollamaServer.port(), "llama3.2:1b");
        var generator = new PageGenerator();

        String pageContent = scraper.scrape(restaurant.url());
        String menu = llm.extractMenu(restaurant.name(), pageContent, MONDAY);
        String html = generator.generate(List.of(new LunchResult(restaurant, menu, null)), MONDAY);
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), html);

        String readHtml = Files.readString(outputDir.resolve("index.html"));

        assertTrue(readHtml.contains("Test Bistro"), "Restaurant name missing");
        assertTrue(readHtml.contains("<li>Pasta Carbonara - 105kr</li>"), "Menu item missing");
        assertTrue(readHtml.contains("Röd linssoppa"), "Second menu item missing");
        assertTrue(readHtml.contains("Måndag"), "Weekday header missing");
        assertFalse(readHtml.contains("class=\"error\""), "Should not show error state");

        ollamaServer.verify(postRequestedFor(urlEqualTo("/api/generate"))
                .withRequestBody(containing("Test Bistro"))
                .withRequestBody(containing("måndag")));
    }

    @Test
    void scrapingErrorShowsErrorCardInPage(@TempDir Path outputDir) throws Exception {
        restaurantServer.stubFor(get("/lunch").willReturn(serverError()));

        var restaurant = new Restaurant(
                "Broken Café",
                "http://localhost:" + restaurantServer.port() + "/lunch",
                null
        );

        var result = new LunchResult(restaurant, null, "HTTP error fetching URL");
        String htmlString = new PageGenerator().generate(List.of(result), MONDAY);
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), htmlString);

        String html = Files.readString(outputDir.resolve("index.html"));
        assertTrue(html.contains("Broken Café"));
        assertTrue(html.contains("class=\"error\""));
    }

    @Test
    void scraperStripsScriptAndStyleTags() {
        restaurantServer.stubFor(get("/menu").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <script>var x = secret_token;</script>
                          <style>.foo { color: red }</style>
                          <nav>Home | About</nav>
                          <main>Grillad lax med potatismos 120kr</main>
                        </body></html>
                        """)));

        var content = scraper.scrape("http://localhost:" + restaurantServer.port() + "/menu");

        assertTrue(content.contains("Grillad lax"), "Main content should be present");
        assertFalse(content.contains("secret_token"), "Script content must be stripped");
        assertFalse(content.contains(".foo"), "Style content must be stripped");
    }
}
