package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PageGeneratorTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 20); // a Monday

    private final PageGenerator generator = new PageGenerator();

    @Test
    void generatesHtmlString() {
        String html = generator.generate(List.of(), MONDAY);
        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
    }

    @Test
    void showsWeekdayAndDateInHeader() {
        String html = generator.generate(List.of(), MONDAY);

        assertTrue(html.contains("Måndag"), "Swedish weekday should appear capitalized");
        assertTrue(html.contains("20 april 2026"), "Full date should appear");
    }

    @Test
    void rendersMenuAsListItems() {
        var result = new LunchResult(
                new Restaurant("Kolgrillen", "https://kolgrillen.se/lunch"),
                "Pasta Bolognese 95kr\nVegetarisk lasagne 90kr",
                null
        );

        String html = generator.generate(List.of(result), MONDAY);

        assertTrue(html.contains("Kolgrillen"));
        assertTrue(html.contains("<li>Pasta Bolognese 95kr</li>"));
        assertTrue(html.contains("<li>Vegetarisk lasagne 90kr</li>"));
        assertTrue(html.contains("kolgrillen.se/lunch"));
    }

    @Test
    void showsEmptyStateWhenNoMenuFound() {
        var result = new LunchResult(
                new Restaurant("Stängt", "https://example.com"),
                "Ingen lunch hittad.",
                null
        );

        String html = generator.generate(List.of(result), MONDAY);

        assertTrue(html.contains("class=\"empty\""), "Should use empty style, not error");
        assertFalse(html.contains("<li>"), "Should not render list items for no-menu response");
    }

    @Test
    void escapesHtmlInMenuContent() {
        var result = new LunchResult(
                new Restaurant("Café <Test> & More", "https://example.com"),
                "Menu with <script>alert('xss')</script>",
                null
        );

        String html = generator.generate(List.of(result), MONDAY);

        assertFalse(html.contains("<script>"), "Raw <script> tag must not appear in output");
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("Caf\u00e9 &lt;Test&gt; &amp; More"));
    }

    @Test
    void showsErrorClassWhenScrapingFailed() {
        var result = new LunchResult(
                new Restaurant("Broken", "https://broken.example"),
                null,
                "Connection refused"
        );

        String html = generator.generate(List.of(result), MONDAY);

        assertTrue(html.contains("Connection refused"));
        assertTrue(html.contains("class=\"error\""));
        assertFalse(html.contains("<li>"), "Error card should not contain menu items");
    }

    @Test
    void handlesMultipleRestaurants() {
        var results = List.of(
                new LunchResult(new Restaurant("Alpha", "https://alpha.se"), "Soup", null),
                new LunchResult(new Restaurant("Beta", "https://beta.se"), null, "Timeout"),
                new LunchResult(new Restaurant("Gamma", "https://gamma.se"), "Steak", null)
        );

        String html = generator.generate(results, MONDAY);

        assertTrue(html.contains("Alpha"));
        assertTrue(html.contains("Beta"));
        assertTrue(html.contains("Gamma"));
        assertTrue(html.contains("Timeout"));
        assertTrue(html.contains("<li>Soup</li>"));
        assertTrue(html.contains("<li>Steak</li>"));
    }

    @Test
    void producesValidHtmlStructure() {
        String html = generator.generate(List.of(), MONDAY);

        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("<html"));
        assertTrue(html.contains("</html>"));
        assertTrue(html.contains("<meta charset=\"UTF-8\">"));
    }
}
