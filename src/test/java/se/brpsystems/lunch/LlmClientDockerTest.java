package se.brpsystems.lunch;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests that spin up a real Ollama Docker container via the system `docker` command.
 * First run pulls ollama/ollama + llama3.2:3b (~1.3 GB) — Docker caches both after that.
 *
 * Skip without Docker: mvn test -Dgroups="!docker"
 */
@Tag("docker")
class LlmClientDockerTest {

    private static final String    MODEL     = "llama3.2:3b";
    private static final String    CONTAINER = "lunch-ollama-test";
    private static final int       PORT      = 11435;
    private static final String    ENDPOINT  = "http://localhost:" + PORT;
    private static final LocalDate MONDAY    = LocalDate.of(2026, 4, 20);

    private static LlmClient client;

    @BeforeAll
    static void startOllamaAndPullModel() throws Exception {
        assumeTrue(isDockerAvailable(), "Docker daemon not reachable — skipping Ollama tests");

        docker("rm", "-f", CONTAINER);
        docker("run", "-d", "--name", CONTAINER, "-p", PORT + ":11434", "ollama/ollama:latest");
        waitForOllama(60);

        System.out.println("Pulling " + MODEL + " (slow on first run, cached after that)...");
        dockerInheritIO("exec", CONTAINER, "ollama", "pull", MODEL);

        client = new LlmClient(ENDPOINT, MODEL);
    }

    @AfterAll
    static void stopOllama() {
        try { docker("rm", "-f", CONTAINER); } catch (Exception ignored) {}
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    void extractsTodaysMenuFromSwedishContent() throws Exception {
        String pageText = """
                Veckans lunch
                Måndag 20 april
                Pasta Bolognese med parmesan 105 kr
                Grillad kyckling med rotfrukter 115 kr
                Vegetarisk linssoppa med bröd 90 kr
                Tisdag 21 april
                Köttbullar med gräddsås 105 kr
                """;

        String result = client.extractMenu("Restaurang Test", pageText, MONDAY);

        System.out.println("LLM output:\n" + result);
        assertFalse(result.isBlank(), "Expected non-empty response");
        assertTrue(
                result.toLowerCase().contains("pasta") ||
                result.toLowerCase().contains("kyckling") ||
                result.toLowerCase().contains("linssoppa"),
                "Expected a Monday dish, got: " + result
        );
        assertFalse(result.toLowerCase().contains("köttbullar"),
                "Should not include Tuesday's dish");
    }

    @Test
    void returnsNoMenuFoundForUnrelatedContent() throws Exception {
        String pageText = "Welcome! We are a web design agency. Contact us for a quote.";

        String result = client.extractMenu("Not A Restaurant", pageText, MONDAY);

        System.out.println("LLM output for unrelated page:\n" + result);
        assertFalse(result.isBlank());
        assertTrue(
                result.toLowerCase().contains("ingen") ||
                result.toLowerCase().contains("no lunch") ||
                result.toLowerCase().contains("not found") ||
                result.toLowerCase().contains("could not"),
                "Expected 'no menu found' signal, got: " + result
        );
    }

    @Test
    void fullPipelineWithRealOllama(@TempDir Path outputDir) throws Exception {
        String fakePageContent = """
                Lunchmenyn vecka 16 – Måndag 20 april
                Köttbullar med gräddsås och lingon 105 kr
                Falafel med hummus och pitabröd 95 kr
                Fisksoppa med aioli 110 kr
                """;

        var restaurant = new Restaurant("Bistro Docker", "https://example.com", null);
        String menu = client.extractMenu(restaurant.name(), fakePageContent, MONDAY);

        System.out.println("LLM output:\n" + menu);

        var results = List.of(new LunchResult(restaurant, menu, null));
        String html = new PageGenerator().generate(results, MONDAY);
        Files.createDirectories(outputDir);
        Files.writeString(outputDir.resolve("index.html"), html);

        String readHtml = Files.readString(outputDir.resolve("index.html"));
        assertTrue(readHtml.contains("Bistro Docker"), "Restaurant name missing from HTML");
        assertTrue(readHtml.contains("Måndag"), "Weekday missing from HTML");
        assertFalse(readHtml.contains("class=\"error\""), "Should not show error card");
        assertTrue(
                menu.toLowerCase().contains("köttbullar") ||
                menu.toLowerCase().contains("falafel") ||
                menu.toLowerCase().contains("fisksoppa"),
                "Expected a dish name in LLM output, got: " + menu
        );
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static boolean isDockerAvailable() {
        try {
            return new ProcessBuilder("docker", "info")
                    .redirectErrorStream(true).start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void docker(String... args) throws Exception {
        var cmd = new String[args.length + 1];
        cmd[0] = "docker";
        System.arraycopy(args, 0, cmd, 1, args.length);
        new ProcessBuilder(cmd).redirectErrorStream(true).start().waitFor();
    }

    private static void dockerInheritIO(String... args) throws Exception {
        var cmd = new String[args.length + 1];
        cmd[0] = "docker";
        System.arraycopy(args, 0, cmd, 1, args.length);
        int exit = new ProcessBuilder(cmd).inheritIO().start().waitFor();
        if (exit != 0) throw new RuntimeException("docker command failed (exit " + exit + ")");
    }

    private static void waitForOllama(int timeoutSeconds) throws Exception {
        var http = HttpClient.newHttpClient();
        var deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            try {
                var req = HttpRequest.newBuilder()
                        .uri(URI.create(ENDPOINT + "/api/tags"))
                        .timeout(Duration.ofSeconds(2)).GET().build();
                if (http.send(req, HttpResponse.BodyHandlers.discarding()).statusCode() == 200) return;
            } catch (Exception ignored) {}
            Thread.sleep(2000);
        }
        throw new IllegalStateException("Ollama didn't become ready within " + timeoutSeconds + "s");
    }
}
