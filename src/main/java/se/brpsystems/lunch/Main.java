package se.brpsystems.lunch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
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

    public static void main(String[] args) throws Exception {
        LocalDate date = parseDate(args);
        String configPath = syspropOrEnv("lunch.restaurants", "LUNCH_RESTAURANTS", "restaurants.yaml");
        List<Restaurant> restaurants = loadRestaurants(configPath);
        System.out.printf("Loaded %d restaurants from %s%n", restaurants.size(), configPath);
        System.out.printf("Date: %s (%s)%n", date, date.getDayOfWeek());

        List<LunchResult> results;
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of("--no-sandbox", "--disable-dev-shm-usage")));
            results = getLunchResults(restaurants, browser, date.getDayOfWeek());
            browser.close();
        }

        for (LunchResult result : results) {
            System.out.printf("- %s: %s%n", result.restaurant().name(),
                    result.success() ? result.dishes().size() + " dishes" : "ERROR: " + result.error());
        }

        var byLocation = new LinkedHashMap<String, List<LunchResult>>();
        for (var result : results) {
            byLocation.computeIfAbsent(result.restaurant().location(), k -> new ArrayList<>()).add(result);
        }
        byLocation.values().forEach(Collections::shuffle);

        String githubRepo = syspropOrEnv("github.repository", "GITHUB_REPOSITORY", "");
        var generator = new PageGenerator();

        Path outputDir = Path.of("output");
        Files.createDirectories(outputDir);

        Files.writeString(outputDir.resolve("index.html"),
                generator.generateIndex(byLocation.keySet(), date, githubRepo));
        System.out.println("Wrote output/index.html");

        for (var entry : byLocation.entrySet()) {
            String slug = toSlug(entry.getKey());
            Path locationDir = outputDir.resolve(slug);
            Files.createDirectories(locationDir);
            Files.writeString(locationDir.resolve("index.html"),
                    generator.generateLocationPage(entry.getKey(), entry.getValue(), date, githubRepo));
            System.out.println("Wrote output/" + slug + "/index.html");
        }
    }

    private static List<LunchResult> getLunchResults(
            List<Restaurant> restaurants, Browser browser, DayOfWeek day) {
        var results = new ArrayList<LunchResult>();
        for (Restaurant restaurant : restaurants) {
            System.out.printf("Scraping: %s (%s)%n", restaurant.name(), restaurant.url());
            try (Page page = browser.newPage()) {
                MenuScraper scraper = ScraperRegistry.get(restaurant.name());
                WeeklyMenu weeklyMenu = scraper.scrape(page, restaurant.url());
                List<Dish> dishes = weeklyMenu.forDay(day);
                System.out.printf("  Got %d dishes.%n", dishes.size());
                results.add(new LunchResult(restaurant, dishes, null));
            } catch (Exception e) {
                System.err.printf("  Failed: %s%n", e.getMessage());
                results.add(new LunchResult(restaurant, null, e.getMessage()));
            }
        }
        return results;
    }

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
                return LocalDate.parse(args[i + 1]);
            }
        }
        return LocalDate.now(ZoneId.of("Europe/Stockholm"));
    }
}
