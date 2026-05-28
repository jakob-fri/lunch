package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HusmanScraper extends BaseMenuScraper {

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
        // Husman: Elementor heading widgets in e-con-inner containers
        // First heading in each day group = "Måndag 25 maj" etc., subsequent = dishes
        // We use JS to collect all elementor-heading-title spans and group them
        List<String> titles = (List<String>) page.evaluate("""
                (() => {
                    const spans = document.querySelectorAll('.elementor-heading-title');
                    return Array.from(spans).map(s => s.innerText.trim()).filter(t => t.length > 0);
                })()
                """);

        if (titles == null || titles.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();
        DayOfWeek current = null;
        // Skip non-day, non-dish entries like "Välkommen!"
        for (String title : titles) {
            DayOfWeek day = detectDay(title);
            if (day != null) {
                current = day;
                byDay.putIfAbsent(current, new ArrayList<>());
            } else if (current != null && !isNoise(title)) {
                byDay.get(current).add(new Dish(title));
            }
        }

        byDay.values().removeIf(List::isEmpty);
        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }

    private DayOfWeek detectDay(String title) {
        // Day headings are like "Måndag 25 maj", "Tisdag 26 maj", etc.
        String lower = title.toLowerCase();
        for (Map.Entry<String, DayOfWeek> entry : DAYS.entrySet()) {
            if (lower.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isNoise(String title) {
        // Filter out known non-dish lines
        String lower = title.toLowerCase();
        return lower.equals("välkommen!") || lower.equals("välkommen");
    }
}
