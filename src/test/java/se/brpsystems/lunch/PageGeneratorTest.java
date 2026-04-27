package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PageGeneratorTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 4, 20);

    private final PageGenerator generator = new PageGenerator();

    private static Restaurant r(String name, String url) {
        return new Restaurant(name, url, null);
    }

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
                r("Kolgrillen", "https://kolgrillen.se/lunch"),
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
                r("Stängt", "https://example.com"),
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
                r("Café <Test> & More", "https://example.com"),
                "Menu with <script>alert('xss')</script>",
                null
        );

        String html = generator.generate(List.of(result), MONDAY);

        assertFalse(html.contains("<script>"), "Raw <script> tag must not appear in output");
        assertTrue(html.contains("&lt;script&gt;"));
        assertTrue(html.contains("Café &lt;Test&gt; &amp; More"));
    }

    @Test
    void showsErrorClassWhenScrapingFailed() {
        var result = new LunchResult(
                r("Broken", "https://broken.example"),
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
                new LunchResult(r("Alpha", "https://alpha.se"), "Soup", null),
                new LunchResult(r("Beta", "https://beta.se"), null, "Timeout"),
                new LunchResult(r("Gamma", "https://gamma.se"), "Steak", null)
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

    @Test
    void generateIndexContainsLocationLinks() {
        String html = generator.generateIndex(
                Set.of("Innerstaden Linköping"),
                MONDAY,
                ""
        );

        assertTrue(html.contains("innerstaden-linkoping/"), "Should contain slug href");
        assertTrue(html.contains("Innerstaden Linköping"), "Should contain location name");
        assertTrue(html.contains("location-card"), "Should use location-card style");
    }

    @Test
    void generateLocationPageContainsBackLink() {
        var results = List.of(new LunchResult(r("Grand", "https://grand.se"), "Soup", null));
        String html = generator.generateLocationPage("Innerstaden Linköping", results, MONDAY, "");

        assertTrue(html.contains("href=\"../\""), "Should link back to root");
        assertTrue(html.contains("Alla platser"), "Should have back-link text");
    }

    @Test
    void generateLocationPageRendersCards() {
        var results = List.of(
                new LunchResult(r("Grand", "https://grand.se"), "Pasta 95kr", null),
                new LunchResult(r("Yogi", "https://yogi.se"), "Curry 90kr", null)
        );
        String html = generator.generateLocationPage("Innerstaden Linköping", results, MONDAY, "");

        assertTrue(html.contains("Grand"));
        assertTrue(html.contains("Yogi"));
        assertTrue(html.contains("<li>Pasta 95kr</li>"));
        assertTrue(html.contains("<li>Curry 90kr</li>"));
        assertTrue(html.contains("Innerstaden Linköping"));
    }
}
