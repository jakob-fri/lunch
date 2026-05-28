package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class DefaultMenuScraperTest extends ScraperTestBase {

    private final DefaultMenuScraper scraper = new DefaultMenuScraper();

    @Test
    void extractsDayGroupedMenu() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <h2>Måndag</h2>
                          <ul><li>Pasta Carbonara 105kr</li><li>Röd linssoppa 90kr</li></ul>
                          <h2>Tisdag</h2>
                          <ul><li>Grillad lax 120kr</li></ul>
                        </body></html>
                        """)));

        WeeklyMenu menu = scraper.scrape(page, server.url("/lunch"));

        assertEquals(2, menu.forDay(DayOfWeek.MONDAY).size());
        assertEquals("Pasta Carbonara 105kr", menu.forDay(DayOfWeek.MONDAY).get(0).description());
        assertEquals("Röd linssoppa 90kr", menu.forDay(DayOfWeek.MONDAY).get(1).description());
        assertEquals(1, menu.forDay(DayOfWeek.TUESDAY).size());
        assertEquals("Grillad lax 120kr", menu.forDay(DayOfWeek.TUESDAY).get(0).description());
    }

    @Test
    void fallsBackToAllWeekWhenNoDayNames() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <ul>
                            <li>Pasta Carbonara 105kr</li>
                            <li>Röd linssoppa 90kr</li>
                          </ul>
                        </body></html>
                        """)));

        WeeklyMenu menu = scraper.scrape(page, server.url("/lunch"));

        // All-week: same list for any day
        assertEquals(2, menu.forDay(DayOfWeek.MONDAY).size());
        assertEquals(menu.forDay(DayOfWeek.MONDAY), menu.forDay(DayOfWeek.FRIDAY));
    }

    @Test
    void returnsEmptyForPageWithNoRelevantContent() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("<html><body></body></html>")));

        WeeklyMenu menu = scraper.scrape(page, server.url("/lunch"));

        assertTrue(menu.isEmpty());
    }

    @Test
    void doesNotIncludeEmptyDayGroups() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <h2>Måndag</h2>
                          <ul><li>Pasta 95kr</li></ul>
                          <h2>Tisdag</h2>
                        </body></html>
                        """)));

        WeeklyMenu menu = scraper.scrape(page, server.url("/lunch"));

        assertFalse(menu.forDay(DayOfWeek.MONDAY).isEmpty());
        // Tisdag has no items — should not appear as an empty entry
        assertTrue(menu.forDay(DayOfWeek.TUESDAY).isEmpty());
    }
}
