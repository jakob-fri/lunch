package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VonDufvaScraper extends BaseMenuScraper {

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
        // Von Dufva (Stadsmissionen): Nuxt Wysiwyg block
        // Structure: <strong>MÅNDAG</strong> in <p>, followed by <p> with dish text
        // Days are uppercase: MÅNDAG, TISDAG, etc.
        List<Object> rows = (List<Object>) page.evaluate("""
                (() => {
                    const DAYS = {
                        'måndag': 'måndag', 'tisdag': 'tisdag', 'onsdag': 'onsdag',
                        'torsdag': 'torsdag', 'fredag': 'fredag'
                    };
                    const rows = [];
                    // Find the Wysiwyg block
                    const container = document.querySelector('[data-block="Wysiwyg"]');
                    if (!container) return rows;
                    const elements = container.querySelectorAll('p, strong');
                    // Walk through <p> elements inside container
                    const pElements = container.querySelectorAll('p');
                    pElements.forEach(p => {
                        const text = p.innerText.trim();
                        if (!text || text === '\\u00a0') return;
                        // Check if this p contains only a <strong> with a day name
                        const strongEl = p.querySelector('strong');
                        if (strongEl) {
                            const strongText = strongEl.innerText.trim().toLowerCase();
                            if (DAYS[strongText]) {
                                rows.push({ type: 'day', name: strongText });
                                return;
                            }
                        }
                        rows.push({ type: 'item', text });
                    });
                    return rows;
                })()
                """);

        if (rows == null || rows.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();
        DayOfWeek current = null;
        // We accumulate text lines per day, combining multi-line dishes
        Map<DayOfWeek, StringBuilder> dayBuffers = new LinkedHashMap<>();

        for (Object rowObj : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) rowObj;
            if ("day".equals(row.get("type"))) {
                current = DAYS.get(row.get("name"));
                if (current != null) {
                    dayBuffers.putIfAbsent(current, new StringBuilder());
                }
            } else if (current != null && "item".equals(row.get("type"))) {
                String text = (String) row.get("text");
                if (text != null && !text.isBlank()) {
                    StringBuilder sb = dayBuffers.get(current);
                    if (sb != null) {
                        if (sb.length() > 0) sb.append(" ");
                        sb.append(text);
                    }
                }
            }
        }

        // Convert buffers to single dish per day
        for (Map.Entry<DayOfWeek, StringBuilder> entry : dayBuffers.entrySet()) {
            String text = entry.getValue().toString().trim();
            if (!text.isBlank()) {
                byDay.put(entry.getKey(), List.of(new Dish(text)));
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
