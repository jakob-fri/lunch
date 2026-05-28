package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class YogiScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAY_IDS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    protected WeeklyMenu extract(Page page) {
        // Yogi: .productGroup contains h2[id=dayname] + .products > .product > .productDescription
        List<ElementHandle> groups = page.querySelectorAll(".productGroup");
        if (groups.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();

        for (ElementHandle group : groups) {
            ElementHandle h2 = group.querySelector("h2[id]");
            if (h2 == null) continue;

            String dayId = h2.getAttribute("id");
            if (dayId == null) continue;
            DayOfWeek day = DAY_IDS.get(dayId.toLowerCase());
            if (day == null) continue;

            List<ElementHandle> descriptions = group.querySelectorAll(".productDescription");
            List<Dish> dishes = new ArrayList<>();
            for (ElementHandle desc : descriptions) {
                // h3 = title, p = accompaniment
                ElementHandle titleEl = desc.querySelector("h3");
                if (titleEl == null) continue;
                String title = titleEl.innerText().trim();
                if (title.isBlank()) continue;

                // include accompaniment if present
                ElementHandle pEl = desc.querySelector("p");
                String full = pEl != null && !pEl.innerText().trim().isBlank()
                        ? title + " - " + pEl.innerText().trim()
                        : title;
                dishes.add(new Dish(full));
            }

            if (!dishes.isEmpty()) {
                byDay.put(day, dishes);
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
