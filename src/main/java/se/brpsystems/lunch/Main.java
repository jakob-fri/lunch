package se.brpsystems.lunch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<List<LocationGroup>> GROUP_LIST = new TypeReference<>() {};

    private static List<Restaurant> loadRestaurants(String location) throws Exception {
        Path file = Path.of(location);
        List<LocationGroup> groups;
        if (Files.exists(file)) {
            groups = YAML.readValue(file.toFile(), GROUP_LIST);
        } else {
            InputStream resource = Main.class.getResourceAsStream("/" + location);
            if (resource == null) throw new IllegalArgumentException("restaurants config not found: " + location);
            groups = YAML.readValue(resource, GROUP_LIST);
        }
        return groups.stream()
                .flatMap(g -> g.restaurants().stream()
                        .map(r -> new Restaurant(r.name(), r.url(), g.location())))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws Exception {
        LocalDate date = parseDate(args);
        String configPath = syspropOrEnv("lunch.restaurants", "LUNCH_RESTAURANTS", "restaurants.yaml");
        List<Restaurant> restaurants = loadRestaurants(configPath);
        System.out.println("Loaded " + restaurants.size() + " restaurants from " + configPath);

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

        var generator = new PageGenerator();
        ArrayList<LunchResult> results;
        try (var scraper = new LunchScraper()) {
            results = getLunchResults(restaurants, scraper, llm, date);
        }

        for (LunchResult result : results) {
            System.out.println("- " + result.restaurant().name() + ": " +
                    (result.success() ? "OK" : "ERROR: " + result.error()));
            System.out.println("  " + result.menu());
        }

        // Group by location (preserving YAML order), shuffle within each group
        var byLocation = new LinkedHashMap<String, List<LunchResult>>();
        for (var result : results) {
            byLocation.computeIfAbsent(result.restaurant().location(), k -> new ArrayList<>()).add(result);
        }
        byLocation.values().forEach(Collections::shuffle);

        String githubRepo = syspropOrEnv("github.repository", "GITHUB_REPOSITORY", "");

        Path outputDir = Path.of("output");
        Files.createDirectories(outputDir);

        // Main index page
        String indexHtml = generator.generateIndex(byLocation.keySet(), date, githubRepo);
        Files.writeString(outputDir.resolve("index.html"), indexHtml);
        System.out.println("Wrote output/index.html");

        // One page per location
        for (var entry : byLocation.entrySet()) {
            String slug = toSlug(entry.getKey());
            Path locationDir = outputDir.resolve(slug);
            Files.createDirectories(locationDir);
            String locationHtml = generator.generateLocationPage(entry.getKey(), entry.getValue(), date, githubRepo);
            Files.writeString(locationDir.resolve("index.html"), locationHtml);
            System.out.println("Wrote output/" + slug + "/index.html");
        }
    }

    private static ArrayList<LunchResult> getLunchResults(List<Restaurant> restaurants, LunchScraper scraper, MenuExtractor llm, LocalDate date) {
        var results = new ArrayList<LunchResult>();

        for (Restaurant restaurant : restaurants) {
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

    static String toSlug(String location) {
        return location.toLowerCase()
                .replace("å", "a").replace("ä", "a").replace("ö", "o")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
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
