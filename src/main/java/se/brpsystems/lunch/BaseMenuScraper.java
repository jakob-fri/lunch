package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.WaitUntilState;

import java.util.List;

public abstract class BaseMenuScraper implements MenuScraper {

    @Override
    public final WeeklyMenu scrape(Page page, String url) {
        navigate(page, url);
        return extract(page);
    }

    protected abstract WeeklyMenu extract(Page page);

    protected void navigate(Page page, String url) {
        Response response;
        try {
            response = page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(15_000)
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
        } catch (PlaywrightException e) {
            if (!e.getMessage().contains("Timeout")) throw e;
            try {
                response = page.navigate(url, new Page.NavigateOptions()
                        .setTimeout(30_000)
                        .setWaitUntil(WaitUntilState.LOAD));
            } catch (PlaywrightException e2) {
                if (!e2.getMessage().contains("Timeout")) throw e2;
                response = page.navigate(url, new Page.NavigateOptions()
                        .setTimeout(30_000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            }
        }
        if (response != null && !response.ok()) {
            throw new RuntimeException("HTTP error fetching URL: " + response.status() + " " + url);
        }
    }

    protected List<Dish> dishesOf(Page page, String cssSelector) {
        return page.querySelectorAll(cssSelector).stream()
                .map(ElementHandle::innerText)
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .map(Dish::new)
                .toList();
    }

    protected List<String> textsOf(Page page, String cssSelector) {
        return page.querySelectorAll(cssSelector).stream()
                .map(ElementHandle::innerText)
                .map(String::trim)
                .filter(t -> !t.isBlank())
                .toList();
    }
}
