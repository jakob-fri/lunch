package se.brpsystems.lunch;

import com.microsoft.playwright.Page;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;

public class OkSmakOchTakScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> SWEDISH_DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY, "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY, "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    @SuppressWarnings("unchecked")
    protected WeeklyMenu extract(Page page) {
        int currentWeek = LocalDate.now(ZoneId.of("Europe/Stockholm"))
                .get(WeekFields.ISO.weekOfWeekBasedYear());

        // The page shows multiple weeks at once (e.g. "LUNCHMENY VECKA 22", "LUNCH Vecka 23").
        // We only collect days/dishes from the section matching the current ISO week number.
        List<Object> rows = (List<Object>) page.evaluate("""
                (() => {
                    const DAYS = ['måndag','tisdag','onsdag','torsdag','fredag'];
                    const CURRENT_WEEK = %d;
                    const rows = [];
                    let inCurrentWeek = false;
                    document.querySelectorAll('h1,h2,h3,h4,h5,h6,p').forEach(el => {
                        const text = (el.innerText || '').trim().replace(/\\u00a0/g, ' ').trim();
                        if (!text) return;
                        const lower = text.toLowerCase();
                        const weekMatch = lower.match(/vecka\\s+(\\d+)/);
                        if (weekMatch) {
                            inCurrentWeek = (parseInt(weekMatch[1]) === CURRENT_WEEK);
                            return;
                        }
                        if (!inCurrentWeek) return;
                        const day = DAYS.find(d => lower === d
                                || lower.startsWith(d + ' ')
                                || lower.startsWith(d + ':'));
                        if (day) {
                            rows.push({ type: 'day', name: day });
                        } else if (el.tagName === 'P' && text.length > 5) {
                            const lines = text.split('\\n')
                                .map(l => l.trim().replace(/\\u00a0/g, ' ').trim())
                                .filter(l => l.length > 5);
                            if (lines.length > 0) rows.push({ type: 'items', lines });
                        }
                    });
                    return rows;
                })()
                """.formatted(currentWeek));

        if (rows == null || rows.isEmpty()) return WeeklyMenu.empty();

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();
        DayOfWeek current = null;

        for (Object rowObj : rows) {
            Map<String, Object> row = (Map<String, Object>) rowObj;
            if ("day".equals(row.get("type"))) {
                current = SWEDISH_DAYS.get(row.get("name"));
                if (current != null) byDay.putIfAbsent(current, new ArrayList<>());
            } else if ("items".equals(row.get("type")) && current != null) {
                List<String> lines = (List<String>) row.get("lines");
                if (lines != null) {
                    for (String line : lines) {
                        byDay.computeIfAbsent(current, k -> new ArrayList<>()).add(new Dish(line));
                    }
                }
            }
        }

        byDay.values().removeIf(List::isEmpty);
        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
