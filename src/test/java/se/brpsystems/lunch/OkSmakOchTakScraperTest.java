package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.util.EnumSet;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class OkSmakOchTakScraperTest extends ScraperTestBase {
    @Test
    void extractsDishesFromFixture() {
        server.stubFor(get("/lunchmeny/").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("ok-smak-och-tak.html")));

        WeeklyMenu menu = new OkSmakOchTakScraper().scrape(page, server.url("/lunchmeny/"));

        assertFalse(menu.isEmpty(), "OkSmakOchTakScraper returned empty menu from fixture");
        boolean hasDishes = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                .stream().anyMatch(day -> !menu.forDay(day).isEmpty());
        assertTrue(hasDishes, "Expected dishes on at least one weekday");
    }
}
