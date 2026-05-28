package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClarasCoffeeScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    protected WeeklyMenu extract(Page page) {
        // Claras Coffee: day header is h5 in .bg-primary-opaque div,
        // dishes are h5.inline elements within the same outer day container
        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();

        // Each day block: div.mt-4.flex-col > div.bg-primary-opaque > h5 (day name)
        //                                   > following sibling div > div.p-2 > h5.inline (dish name)
        List<ElementHandle> dayHeaders = page.querySelectorAll(".bg-primary-opaque h5");
        for (ElementHandle header : dayHeaders) {
            String dayText = header.innerText().trim();
            DayOfWeek day = DAYS.get(dayText.toLowerCase());
            if (day == null) continue;

            // The dish items are siblings of the header's parent container
            // Navigate up to the day container, then find dish titles
            ElementHandle dayContainer = (ElementHandle) page.evaluateHandle(
                    "(el) => el.closest('.flex.flex-col.gap-y-2.md\\\\:gap-y-4')", header);
            if (dayContainer == null) continue;

            // Dish names: h5.inline within .break-words class (dish title h5)
            List<ElementHandle> dishEls = dayContainer.querySelectorAll("h5.inline.whitespace-pre-wrap.break-words");
            List<Dish> dishes = new ArrayList<>();
            for (ElementHandle dishEl : dishEls) {
                String dishName = dishEl.innerText().trim();
                if (!dishName.isBlank()) {
                    dishes.add(new Dish(dishName));
                }
            }
            if (!dishes.isEmpty()) {
                byDay.put(day, dishes);
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
