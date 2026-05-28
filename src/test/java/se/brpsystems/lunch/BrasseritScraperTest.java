package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;
import java.time.DayOfWeek;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class BrasseritScraperTest extends ScraperTestBase {
    @Test
    void extractsDishesFromFixture() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("brasserit.html")));

        WeeklyMenu menu = new BrasseritScraper().scrape(page, server.url("/lunch"));

        assertFalse(menu.isEmpty(), "BrasseritScraper returned empty menu from fixture");
    }

    @Test
    void returnsSameMenuForAnyWeekday() {
        server.stubFor(get("/lunch").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBodyFile("brasserit.html")));
        WeeklyMenu menu = new BrasseritScraper().scrape(page, server.url("/lunch"));
        assertEquals(menu.forDay(DayOfWeek.MONDAY), menu.forDay(DayOfWeek.FRIDAY),
                "All-week menu should be same for any weekday");
    }
}
