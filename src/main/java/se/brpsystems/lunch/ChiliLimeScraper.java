package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ChiliLimeScraper extends BaseMenuScraper {

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
        // Chili Lime: Old HTML table site
        // .underRubrik = day name (inside a <span>), the whole <p> contains all dishes for that day
        List<Object> rows = (List<Object>) page.evaluate("""
                (() => {
                    const DAYS = ['måndag','tisdag','onsdag','torsdag','fredag'];
                    const rows = [];
                    // Find all <p> elements containing .underRubrik spans
                    const allP = document.querySelectorAll('p');
                    allP.forEach(p => {
                        const span = p.querySelector('.underRubrik');
                        if (span) {
                            const dayText = span.innerText.trim().toLowerCase();
                            if (DAYS.includes(dayText)) {
                                // Get all text content of this paragraph, line by line
                                const fullText = p.innerText.trim();
                                rows.push({ type: 'dayblock', day: dayText, text: fullText });
                            }
                        }
                    });
                    return rows;
                })()
                """);

        if (rows == null || rows.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();

        for (Object rowObj : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) rowObj;
            String dayName = (String) row.get("day");
            String fullText = (String) row.get("text");
            DayOfWeek day = DAYS.get(dayName);
            if (day == null || fullText == null) continue;

            // The text starts with the day name, then contains numbered dishes
            // Split on line breaks and number prefixes
            List<Dish> dishes = new ArrayList<>();
            String[] lines = fullText.split("\n");
            StringBuilder current = new StringBuilder();
            boolean pastDayHeader = false;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isBlank()) continue;
                // Skip the day name header line
                if (!pastDayHeader) {
                    if (trimmed.toLowerCase().equals(dayName)) {
                        pastDayHeader = true;
                        continue;
                    }
                }
                // Lines starting with a number like "1.", "2.", "3." are dish entries
                if (trimmed.matches("^\\d+\\..*") || trimmed.matches("^[A-Z]\\. .*")) {
                    if (current.length() > 0) {
                        String dish = current.toString().trim();
                        if (!dish.isBlank() && !dish.matches("^\\d+\\.?\\s*$")) {
                            dishes.add(new Dish(dish));
                        }
                        current.setLength(0);
                    }
                    current.append(trimmed);
                } else if (current.length() > 0) {
                    current.append(" ").append(trimmed);
                } else if (pastDayHeader && !trimmed.isBlank()) {
                    current.append(trimmed);
                }
            }
            if (current.length() > 0) {
                String dish = current.toString().trim();
                if (!dish.isBlank() && !dish.matches("^\\d+\\.?\\s*$")) {
                    dishes.add(new Dish(dish));
                }
            }

            if (!dishes.isEmpty()) {
                byDay.put(day, dishes);
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
