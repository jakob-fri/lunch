package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.util.EnumSet;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class BistroCominhScraperTest extends ScraperTestBase {
    @Test
    void extractsDishesFromFixture() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("bistro-cominh.html")));

        WeeklyMenu menu = new BistroCominhScraper().scrape(page, server.url("/lunch"));

        assertFalse(menu.isEmpty(), "BistroCominhScraper returned empty menu from fixture");
        boolean hasDishes = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                .stream().anyMatch(day -> !menu.forDay(day).isEmpty());
        assertTrue(hasDishes, "Expected dishes on at least one weekday");
    }
}
