package se.brpsystems.lunch;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import java.util.List;

public class LunchScraper implements AutoCloseable {

    private static final int MAX_CHARS = 8000;

    private final Playwright playwright;
    private final Browser browser;

    public LunchScraper() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
    }

    public String scrape(String url) {
        try (Page page = browser.newPage()) {
            page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(30_000)
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));

            String text = (String) page.evaluate("""
                    (() => {
                        document.querySelectorAll('script,style,nav,footer,header,iframe,noscript').forEach(e => e.remove());
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
    }

    @Override
    public void close() {
        browser.close();
        playwright.close();
    }
}
