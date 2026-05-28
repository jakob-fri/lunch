package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GrandScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    protected WeeklyMenu extract(Page page) {
        // Grand uses Squarespace menu block: .menu-item-title = day name, .menu-item-description = dish
        List<ElementHandle> items = page.querySelectorAll(".menu-item");
        if (items.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<se.brpsystems.lunch.Dish>> byDay = new LinkedHashMap<>();

        for (ElementHandle item : items) {
            ElementHandle titleEl = item.querySelector(".menu-item-title");
            ElementHandle descEl = item.querySelector(".menu-item-description");
            if (titleEl == null || descEl == null) continue;

            String title = titleEl.innerText().trim();
            String desc = descEl.innerText().trim();
            if (desc.isBlank()) continue;

            DayOfWeek day = DAYS.get(title.toLowerCase());
            if (day != null) {
                byDay.computeIfAbsent(day, k -> new java.util.ArrayList<>())
                        .add(new Dish(desc));
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
