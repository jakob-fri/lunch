package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.util.EnumSet;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class HusmanScraperTest extends ScraperTestBase {
    @Test
    void extractsDishesFromFixture() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("husman.html")));

        WeeklyMenu menu = new HusmanScraper().scrape(page, server.url("/lunch"));

        assertFalse(menu.isEmpty(), "HusmanScraper returned empty menu from fixture");
        boolean hasDishesOnAWeekday = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                .stream().anyMatch(day -> !menu.forDay(day).isEmpty());
        assertTrue(hasDishesOnAWeekday, "Expected at least one weekday with dishes");
    }
}
