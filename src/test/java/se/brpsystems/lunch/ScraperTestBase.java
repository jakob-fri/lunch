package se.brpsystems.lunch;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

abstract class ScraperTestBase {

    protected static WireMockServer server;
    protected static Playwright playwright;
    protected static Browser browser;
    protected static Page page;

    @BeforeAll
    static void startInfrastructure() {
        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
        page = browser.newPage();
    }

    @AfterAll
    static void stopInfrastructure() {
        page.close();
        browser.close();
        playwright.close();
        server.stop();
    }

    @BeforeEach
    void resetStubs() {
        server.resetAll();
    }
}
