package se.brpsystems.lunch;

import com.microsoft.playwright.Page;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

class BaseMenuScraperTest extends ScraperTestBase {

    @Test
    void dishesOfExtractsMatchingElements() {
        server.stubFor(get("/menu").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("""
                        <html><body>
                          <ul><li>Pasta Carbonara</li><li>Röd linssoppa</li></ul>
                        </body></html>
                        """)));

        var collected = new ArrayList<Dish>();
        var scraper = new BaseMenuScraper() {
            @Override
            protected WeeklyMenu extract(Page p) {
                collected.addAll(dishesOf(p, "li"));
                return WeeklyMenu.empty();
            }
        };
        scraper.scrape(page, server.url("/menu"));

        assertEquals(2, collected.size());
        assertEquals("Pasta Carbonara", collected.get(0).description());
        assertEquals("Röd linssoppa", collected.get(1).description());
    }

    @Test
    void dishesOfIgnoresBlanks() {
        server.stubFor(get("/menu").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("<html><body><ul><li>  </li><li>Soup</li></ul></body></html>")));

        var collected = new ArrayList<Dish>();
        var scraper = new BaseMenuScraper() {
            @Override
            protected WeeklyMenu extract(Page p) {
                collected.addAll(dishesOf(p, "li"));
                return WeeklyMenu.empty();
            }
        };
        scraper.scrape(page, server.url("/menu"));

        assertEquals(1, collected.size());
        assertEquals("Soup", collected.get(0).description());
    }

    @Test
    void textsOfReturnsStrings() {
        server.stubFor(get("/menu").willReturn(ok()
                .withHeader("Content-Type", "text/html; charset=utf-8")
                .withBody("<html><body><p>Hello</p><p>World</p></body></html>")));

        var texts = new ArrayList<String>();
        var scraper = new BaseMenuScraper() {
            @Override
            protected WeeklyMenu extract(Page p) {
                texts.addAll(textsOf(p, "p"));
                return WeeklyMenu.empty();
            }
        };
        scraper.scrape(page, server.url("/menu"));

        assertEquals(List.of("Hello", "World"), texts);
    }
}
