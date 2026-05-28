package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import java.util.EnumSet;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class ClarasCoffeeScraperTest extends ScraperTestBase {
    @Test
    void extractsDishesFromFixture() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("claras-coffee.html")));

        WeeklyMenu menu = new ClarasCoffeeScraper().scrape(page, server.url("/lunch"));

        assertFalse(menu.isEmpty(), "ClarasCoffeeScraper returned empty menu from fixture");
        boolean hasDishesOnAWeekday = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY)
                .stream().anyMatch(day -> !menu.forDay(day).isEmpty());
        assertTrue(hasDishesOnAWeekday, "Expected at least one weekday with dishes");
    }
}
