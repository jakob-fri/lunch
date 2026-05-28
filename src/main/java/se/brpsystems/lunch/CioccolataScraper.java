package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CioccolataScraper extends BaseMenuScraper {

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
        // Cioccolata: WordPress/Elementor text-editor widget
        // Multiple text-editor widgets on page; we need the one with h3 day names
        // h3 = day name (e.g. "Måndag:"), followed by <p> = dish descriptions
        List<Object> rows = (List<Object>) page.evaluate("""
                (() => {
                    const DAYS = {
                        'måndag': 'måndag', 'tisdag': 'tisdag', 'onsdag': 'onsdag',
                        'torsdag': 'torsdag', 'fredag': 'fredag'
                    };
                    const rows = [];
                    // Find the elementor text-editor widget that contains day h3 headings
                    let container = null;
                    const allWidgets = document.querySelectorAll('.elementor-widget-text-editor .elementor-widget-container');
                    for (const w of allWidgets) {
                        if (w.querySelector('h3')) {
                            const h3Text = w.querySelector('h3').innerText.trim()
                                .replace(/[:\\u00a0]/g, '').trim().toLowerCase();
                            if (DAYS[h3Text]) {
                                container = w;
                                break;
                            }
                        }
                    }
                    if (!container) return rows;
                    const children = container.querySelectorAll('h3, p');
                    children.forEach(el => {
                        const text = el.innerText.trim().replace(/[:\\u00a0]/g, '').trim().toLowerCase();
                        if (el.tagName === 'H3') {
                            if (DAYS[text]) {
                                rows.push({ type: 'day', name: text });
                            }
                        } else if (el.tagName === 'P') {
                            const full = el.innerText.trim();
                            if (full && full !== '\\u00a0') {
                                rows.push({ type: 'item', text: full });
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
        DayOfWeek current = null;

        for (Object rowObj : rows) {
            @SuppressWarnings("unchecked")
            Map<String, Object> row = (Map<String, Object>) rowObj;
            if ("day".equals(row.get("type"))) {
                current = DAYS.get(row.get("name"));
                if (current != null) {
                    byDay.putIfAbsent(current, new ArrayList<>());
                }
            } else if (current != null && "item".equals(row.get("type"))) {
                String text = (String) row.get("text");
                if (text != null && !text.isBlank()) {
                    byDay.get(current).add(new Dish(text));
                }
            }
        }

        byDay.values().removeIf(List::isEmpty);
        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
