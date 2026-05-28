package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultMenuScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> SWEDISH_DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY,
            "lördag", DayOfWeek.SATURDAY,
            "söndag", DayOfWeek.SUNDAY
    );

    @Override
    @SuppressWarnings("unchecked")
    protected WeeklyMenu extract(Page page) {
        // Each row is either {type:"day", name:"måndag"} or {type:"item", text:"..."}
        List<Object> rows = (List<Object>) page.evaluate("""
                (() => {
                    const DAYS = ['måndag','tisdag','onsdag','torsdag','fredag','lördag','söndag'];
                    const rows = [];
                    document.querySelectorAll('h1,h2,h3,h4,h5,h6,dt,strong,b,li,p').forEach(el => {
                        const text = (el.innerText || '').trim();
                        if (!text || text.length < 3) return;
                        const lower = text.toLowerCase();
                        const day = DAYS.find(d => lower === d
                                || lower.startsWith(d + ' ')
                                || lower.startsWith(d + ':'));
                        if (day) {
                            rows.push({ type: 'day', name: day });
                        } else {
                            rows.push({ type: 'item', text });
                        }
                    });
                    return rows;
                })()
                """);

        if (rows != null && !rows.isEmpty()) {
            Map<DayOfWeek, List<Dish>> byDay = groupByDay(rows);
            if (!byDay.isEmpty()) return WeeklyMenu.perDay(byDay);
        }

        List<Dish> items = dishesOf(page, "li");
        if (!items.isEmpty()) return WeeklyMenu.allWeek(items);

        return WeeklyMenu.allWeek(dishesOf(page, "p"));
    }

    @SuppressWarnings("unchecked")
    private Map<DayOfWeek, List<Dish>> groupByDay(List<Object> rows) {
        Map<DayOfWeek, List<Dish>> result = new LinkedHashMap<>();
        DayOfWeek current = null;

        for (Object rowObj : rows) {
            Map<String, Object> row = (Map<String, Object>) rowObj;
            if ("day".equals(row.get("type"))) {
                current = SWEDISH_DAYS.get(row.get("name"));
                if (current != null) result.putIfAbsent(current, new ArrayList<>());
            } else if (current != null && "item".equals(row.get("type"))) {
                result.get(current).add(new Dish((String) row.get("text")));
            }
        }

        // Discard days that ended up with no items
        result.values().removeIf(List::isEmpty);
        return result;
    }
}
