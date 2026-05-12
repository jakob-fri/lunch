package se.brpsystems.lunch;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import java.util.List;

public class LunchScraper implements AutoCloseable {

    private static final int MAX_CHARS = 8000;
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 3_000;

    private final Playwright playwright;
    private final Browser browser;

    public LunchScraper() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
    }

    public String scrape(String url) {
        PlaywrightException lastException = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try (Page page = browser.newPage()) {
                return doScrape(page, url);
            } catch (PlaywrightException e) {
                lastException = e;
                System.err.printf("Scrape attempt %d/%d failed for %s: %s%n",
                        attempt, MAX_ATTEMPTS, url, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    try { Thread.sleep(RETRY_DELAY_MS); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        throw lastException;
    }

    private String doScrape(Page page, String url) {
        try {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(15_000)
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
        } catch (PlaywrightException e) {
            if (!e.getMessage().contains("Timeout")) throw e;
            try {
                // Site has continuous background activity; settle for load event instead
                page.navigate(url, new Page.NavigateOptions()
                        .setTimeout(30_000)
                        .setWaitUntil(WaitUntilState.LOAD));
            } catch (PlaywrightException e2) {
                if (!e2.getMessage().contains("Timeout")) throw e2;
                // Heavy resources (images/fonts) blocking load; DOM content is enough for text extraction
                page.navigate(url, new Page.NavigateOptions()
                        .setTimeout(30_000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            }
        }

        String text = (String) page.evaluate("""
                (() => {
                    document.querySelectorAll('script,style,nav,footer,header,iframe,noscript').forEach(e => e.remove());
                    // Remove elements hidden via inline opacity (anti-scraping watermarks)
                    document.querySelectorAll('[style]').forEach(e => {
                        const op = e.style.opacity;
                        if (op !== '' && parseFloat(op) < 0.4) e.remove();
                    });
                    return document.body.innerText;
                })()
                """);

        if (text == null || text.isBlank()) return "";

        text = text.replaceAll("[ \t]+", " ")
                   .replaceAll("(?m)^ +", "")
                   .replaceAll("\n{3,}", "\n\n")
                   .strip();

        return text.length() > MAX_CHARS ? text.substring(0, MAX_CHARS) : text;
    }

    @Override
    public void close() {
        browser.close();
        playwright.close();
    }
}
