package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.util.EnumSet;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class TropikhusetScraperTest extends ScraperTestBase {
    @Test
    void extractsDishesFromFixture() {
        server.stubFor(get("/dagens-lunch/").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("tropikhuset.html")));

        WeeklyMenu menu = new TropikhusetScraper().scrape(page, server.url("/dagens-lunch/"));

        assertFalse(menu.isEmpty(), "TropikhusetScraper returned empty menu from fixture");
        boolean hasDishes = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                .stream().anyMatch(day -> !menu.forDay(day).isEmpty())
                || !menu.allWeek().isEmpty();
        assertTrue(hasDishes, "Expected dishes on at least one day or as all-week");
    }
}
