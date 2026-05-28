package se.brpsystems.lunch;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitUntilState;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev-only utility. Run once to capture rendered HTML from each live restaurant page.
 * Requires: mvn package -DskipTests, then Playwright chromium installed.
 *
 * Usage:
 *   java -cp target/lunch-1.0-SNAPSHOT-jar-with-dependencies.jar se.brpsystems.lunch.FixtureCapture
 */
public class FixtureCapture {

    private static final Map<String, String> TARGETS = new LinkedHashMap<>();
    static {
        TARGETS.put("grand",         "https://www.brasseriegrand.se/lunch");
        TARGETS.put("yogi",          "https://restaurangyogi.com/lunch");
        TARGETS.put("brasserit",     "https://brasseriebouquet.se/lunch");
        TARGETS.put("von-dufva",     "https://stadsmissionenost.se/restaurang-von-dufva/lunch");
        TARGETS.put("cioccolata",    "https://www.cioccolata.nu/lunch/");
        TARGETS.put("claras-coffee", "https://clarascoffee.se/");
        TARGETS.put("ekkallan",      "https://ekkallanmatodryck.se/ekkallan-storgatan/");
        TARGETS.put("husman",        "https://restauranghusman.se/veckans-lunch/");
        TARGETS.put("chili-lime",    "https://www.chili-lime.se/helaveckan.asp");
        TARGETS.put("stangs",        "https://stangsmjardevi.se/");
        TARGETS.put("tropikhuset",   "https://belvederen-tropikhuset.se/dagens-lunch/");
        TARGETS.put("ekoxen",        "https://ekoxen.kvartersmenyn.se/");
        TARGETS.put("ok-smak-och-tak", "https://oksmakochtak.se/lunchmeny/");
        TARGETS.put("bistro-cominh",   "https://www.bistrocominh.se/lunch");
    }

    public static void main(String[] args) throws Exception {
        Path outDir = Path.of("src/test/resources/__files");
        Files.createDirectories(outDir);

        try (Playwright pw = Playwright.create()) {
            Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
            Page page = browser.newPage();

            for (var entry : TARGETS.entrySet()) {
                System.out.printf("Capturing %-20s ... ", entry.getKey());
                try {
                    try {
                        page.navigate(entry.getValue(), new Page.NavigateOptions()
                                .setTimeout(20_000).setWaitUntil(WaitUntilState.NETWORKIDLE));
                    } catch (PlaywrightException e) {
                        page.navigate(entry.getValue(), new Page.NavigateOptions()
                                .setTimeout(30_000).setWaitUntil(WaitUntilState.LOAD));
                    }
                    String html = page.content();
                    Path out = outDir.resolve(entry.getKey() + ".html");
                    Files.writeString(out, html);
                    System.out.printf("saved %d chars → %s%n", html.length(), out);
                } catch (Exception e) {
                    System.out.printf("FAILED: %s%n", e.getMessage());
                }
            }
            browser.close();
        }
    }
}
