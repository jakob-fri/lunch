package se.brpsystems.lunch;

import com.microsoft.playwright.Page;
import java.time.DayOfWeek;
import java.util.*;

public class BistroCominhScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> SWEDISH_DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY, "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY, "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    @SuppressWarnings("unchecked")
    protected WeeklyMenu extract(Page page) {
        // Bootstrap tabs: a.nav-link[role="tab"] for day names, div.tab-pane for content.
        // Each tab-pane has a table with columns: NAMN, BESKRIVNING, PRIS, BILD.
        // We skip the header row (thead) and read tbody tr rows.
        List<Object> result = (List<Object>) page.evaluate("""
                (() => {
                    const tabs = Array.from(document.querySelectorAll('a.nav-link[role="tab"]'));
                    const panes = Array.from(document.querySelectorAll('div.tab-pane'));
                    const out = [];
                    for (let i = 0; i < Math.min(tabs.length, panes.length); i++) {
                        const dayName = tabs[i].innerText.trim().toLowerCase();
                        const rows = Array.from(panes[i].querySelectorAll('tbody tr'));
                        const dishes = [];
                        for (const tr of rows) {
                            const cells = tr.querySelectorAll('td');
                            if (cells.length < 2) continue;
                            const name = (cells[0].innerText || '').trim().replace(/\\n+/g, ' ');
                            const desc = (cells[1].innerText || '').trim().replace(/\\n+/g, ' ');
                            if (!name && !desc) continue;
                            if (name && desc) {
                                dishes.push(name + ' - ' + desc);
                            } else {
                                dishes.push(name || desc);
                            }
                        }
                        if (dishes.length > 0) out.push({ day: dayName, dishes });
                    }
                    return out;
                })()
                """);

        if (result == null || result.isEmpty()) return WeeklyMenu.empty();

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();
        for (Object entry : result) {
            Map<String, Object> map = (Map<String, Object>) entry;
            DayOfWeek day = SWEDISH_DAYS.get(map.get("day"));
            if (day == null) continue;
            List<String> dishes = (List<String>) map.get("dishes");
            if (dishes != null && !dishes.isEmpty()) {
                byDay.put(day, dishes.stream().map(Dish::new).toList());
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
