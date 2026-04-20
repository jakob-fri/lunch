package se.brpsystems.lunch;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public class Main {

    // Add your local restaurants here
    private static final List<Restaurant> RESTAURANTS = List.of(
            new Restaurant("Grand", "https://www.brasseriegrand.se/lunch"),
            new Restaurant("Yogi", "https://restaurangyogi.com/lunch"),
            new Restaurant("Brasserit", "https://brasseriebouquet.se/lunch"),
            new Restaurant("Von dufva", "https://stadsmissionenost.se/restaurang-von-dufva/lunch"),
            new Restaurant("Cioccolata", "https://www.cioccolata.nu/lunch/"),
            new Restaurant("Claras coffee", "https://clarascoffee.se/")
    );

    public static void main(String[] args) throws Exception {
        LocalDate date = parseDate(args);
        // System properties (-D flags) take precedence over env vars, so both Maven and CLI usage work
        boolean mock = "true".equalsIgnoreCase(syspropOrEnv("lunch.mock", "LUNCH_MOCK", ""));

        System.out.println("Date: " + date);

        MenuExtractor llm;
        if (mock) {
            System.out.println("Running in MOCK mode (no Ollama needed)");
            llm = new MockLlmClient();
        } else {
            String ollamaUrl = syspropOrEnv("ollama.url", "OLLAMA_URL", "http://localhost:11434");
            String model    = syspropOrEnv("ollama.model", "OLLAMA_MODEL", "llama3.2:3b");
            System.out.println("Using Ollama at " + ollamaUrl + " with model " + model);
            llm = new LlmClient(ollamaUrl, model);
        }

        var scraper = new LunchScraper();
        var generator = new PageGenerator();
        var results = getLunchResults(scraper, llm, date);
        // print results
        for (LunchResult result : results) {
            System.out.println("- " + result.restaurant().name() + ": " +
                    (result.success() ? "OK" : "ERROR: " + result.error()));
            System.out.println("  " + result.menu());
        }

        String githubRepo = syspropOrEnv("github.repository", "GITHUB_REPOSITORY", "");
        String html = generator.generate(results, date, githubRepo);

        Path outputDir = Path.of("output");
        Files.createDirectories(outputDir);
        Path outputPath = outputDir.resolve("index.html");
        Files.writeString(outputPath, html);
        System.out.println("Wrote " + outputPath.toAbsolutePath());
    }

    private static ArrayList<LunchResult> getLunchResults(LunchScraper scraper, MenuExtractor llm, LocalDate date) {
        var results = new ArrayList<LunchResult>();

        for (Restaurant restaurant : RESTAURANTS) {
            System.out.println("Scraping: " + restaurant.name() + " (" + restaurant.url() + ")");
            try {
                String content = scraper.scrape(restaurant.url());
                System.out.println("  Fetched " + content.length() + " chars, asking LLM...");
                String menu = llm.extractMenu(restaurant.name(), content, date);
                System.out.println("  Done.");
                results.add(new LunchResult(restaurant, menu, null));
            } catch (Exception e) {
                System.err.println("  Failed: " + e.getMessage());
                results.add(new LunchResult(restaurant, null, e.getMessage()));
            }
        }
        return results;
    }

    private static String syspropOrEnv(String sysprop, String envVar, String defaultValue) {
        String v = System.getProperty(sysprop);
        if (v != null && !v.isEmpty()) return v;
        v = System.getenv(envVar);
        return (v != null && !v.isEmpty()) ? v : defaultValue;
    }

    private static LocalDate parseDate(String[] args) {
        for (int i = 0; i < args.length - 1; i++) {
            if ("--date".equals(args[i])) {
                return LocalDate.parse(args[i + 1]); // expects ISO format: 2026-04-21
            }
        }
        return LocalDate.now(ZoneId.of("Europe/Stockholm"));
    }
}
