package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TropikhusetScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    @SuppressWarnings("unchecked")
    protected WeeklyMenu extract(Page page) {
        // Tropikhuset: JetEngine listing grid with an Elementor text-editor widget.
        // Each weekday is a <p> whose first child is a <strong> with the day name (e.g. "Måndag: ").
        // Subsequent <p> elements without a bold day prefix are vegetarian/extra dishes for the same day.
        // We collect all <p> innerTexts from the text-editor widget inside the listing grid.
        List<String> paragraphs = (List<String>) page.evaluate("""
                (() => {
                    const grid = document.querySelector('.jet-listing-grid__item');
                    if (!grid) return [];
                    const editors = grid.querySelectorAll('.elementor-widget-text-editor p');
                    return Array.from(editors)
                            .map(p => p.innerText.trim())
                            .filter(t => t.length > 0);
                })()
                """);

        if (paragraphs == null || paragraphs.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();
        DayOfWeek current = null;

        for (String text : paragraphs) {
            DayOfWeek day = detectDay(text);
            if (day != null) {
                current = day;
                String dish = stripDayPrefix(text);
                if (!dish.isBlank()) {
                    byDay.computeIfAbsent(current, k -> new ArrayList<>()).add(new Dish(dish));
                } else {
                    byDay.putIfAbsent(current, new ArrayList<>());
                }
            } else if (current != null && !isNoise(text)) {
                byDay.computeIfAbsent(current, k -> new ArrayList<>()).add(new Dish(text));
            }
        }

        byDay.values().removeIf(List::isEmpty);
        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }

    private DayOfWeek detectDay(String text) {
        String lower = text.toLowerCase();
        for (Map.Entry<String, DayOfWeek> entry : DAYS.entrySet()) {
            if (lower.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String stripDayPrefix(String text) {
        // Remove day name and colon, e.g. "Måndag: dish" -> "dish"
        int colon = text.indexOf(':');
        if (colon >= 0 && colon < 12) {
            return text.substring(colon + 1).trim();
        }
        return "";
    }

    private boolean isNoise(String text) {
        String lower = text.toLowerCase().trim();
        return lower.startsWith("lunch serveras")
                || lower.startsWith("frukost")
                || lower.isEmpty();
    }
}
