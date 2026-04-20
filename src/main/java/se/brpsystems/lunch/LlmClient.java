package se.brpsystems.lunch;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

public class LlmClient implements MenuExtractor {

    private final String baseUrl;
    private final String model;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final DateTimeFormatter WEEKDAY_FMT =
            DateTimeFormatter.ofPattern("EEEE", new Locale("sv", "SE"));
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("d MMMM yyyy", new Locale("sv", "SE"));

    private static final String[] WEEKDAYS =
            {"måndag", "tisdag", "onsdag", "torsdag", "fredag", "lördag", "söndag"};

    // Lines whose first word matches one of these signal a new permanent/weekly section,
    // not a specific day's dishes — stop extracting when we see them.
    private static final String[] SECTION_STOPS =
            {"veckans", "hela veckan", "à la carte", "helg", "alltid", "classics", "klassiker"};

    public LlmClient(String baseUrl, String model) {
        this.baseUrl = baseUrl;
        this.model = model;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public String extractMenu(String restaurantName, String pageContent, LocalDate date) throws Exception {
        String weekday = date.format(WEEKDAY_FMT);

        // Pre-extract today's section to avoid the LLM having to search through a full week.
        String daySection = extractDaySection(pageContent, weekday);

        if (daySection.isEmpty() && hasAnyDaySection(pageContent)) {
            // Page has weekday structure but not for today (e.g. weekend, wrong week)
            return "Ingen lunch hittad.";
        }

        String contentToSend = daySection.isEmpty() ? pageContent : daySection;
        boolean isWeeklyMenu = daySection.isEmpty();

        int delaySeconds = 5;
        Exception lastException = null;
        for (int i = 0; i < 10; i++) {
            try {
                return callLlm(restaurantName, contentToSend, date, isWeeklyMenu);
            } catch (Exception e) {
                lastException = e;
                System.err.println("  LLM attempt " + (i + 1) + "/10 failed: " + e.getMessage());
                if (i < 9) {
                    Thread.sleep(delaySeconds * 1000L);
                    delaySeconds = Math.min(delaySeconds * 2, 60);
                }
            }
        }
        throw new RuntimeException("All LLM attempts failed: " + lastException.getMessage(), lastException);
    }

    private String callLlm(String restaurantName, String content, LocalDate date, boolean isWeeklyMenu) throws Exception {
        String weekday = date.format(WEEKDAY_FMT);
        String dateStr = date.format(DATE_FMT);

        String prompt = isWeeklyMenu
                ? """
                  List only the dish names from this Swedish restaurant's weekly menu.
                  One dish name per line. No prices, no descriptions, no section headers.
                  If no dishes are found, respond with exactly: Ingen lunch hittad.
                  No other text.

                  Restaurant: %s

                  Menu text:
                  %s
                  """.formatted(restaurantName, content)
                : """
                  Format these Swedish lunch dishes as a clean list for %s (%s).
                  One dish per line. Keep A./B./C. labels if present. No prices.
                  If the text contains no dishes, respond with exactly: Ingen lunch hittad.
                  No other text.

                  Restaurant: %s

                  Dishes:
                  %s
                  """.formatted(weekday, dateStr, restaurantName, content);

        var payload = Map.of(
                "model", model,
                "prompt", prompt,
                "stream", false,
                "options", Map.of("temperature", 0)
        );

        var request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(120))
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(payload)))
                .build();

        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ollama returned HTTP " + response.statusCode());
        }
        return mapper.readTree(response.body()).get("response").asText().strip();
    }

    // Returns content between today's weekday heading and the next weekday or section-stop heading.
    // Returns empty string when no per-day section is found for this weekday.
    private String extractDaySection(String content, String weekday) {
        StringBuilder result = new StringBuilder();
        boolean inTarget = false;

        for (String line : content.split("\n")) {
            String stripped = line.strip();
            String lower = stripped.toLowerCase().replaceAll("[:\\-–].*", "").strip();

            // Check for a weekday heading (exact match after stripping punctuation)
            String matchedDay = null;
            for (String day : WEEKDAYS) {
                if (lower.equals(day)) { matchedDay = day; break; }
            }
            if (matchedDay != null) {
                if (inTarget) break; // hit the next weekday — stop
                inTarget = matchedDay.equals(weekday.toLowerCase());
                continue;
            }

            if (inTarget) {
                // Check for a new permanent section (e.g. "Veckans vegetariska")
                boolean stop = false;
                for (String stopWord : SECTION_STOPS) {
                    if (lower.startsWith(stopWord)) { stop = true; break; }
                }
                if (stop) break;
                if (!stripped.isEmpty()) result.append(stripped).append("\n");
            }
        }

        return result.toString().strip();
    }

    // True if the content has any weekday headings, meaning the page is structured per-day.
    private boolean hasAnyDaySection(String content) {
        for (String line : content.split("\n")) {
            String lower = line.strip().toLowerCase().replaceAll("[:\\-–].*", "").strip();
            for (String day : WEEKDAYS) {
                if (lower.equals(day)) return true;
            }
        }
        return false;
    }
}
