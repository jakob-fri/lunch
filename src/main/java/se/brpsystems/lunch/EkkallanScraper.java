package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EkkallanScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    protected WeeklyMenu extract(Page page) {
        // Ekkällan: Elementor price list widget
        // .elementor-price-list-item > .elementor-price-list-text
        //   .elementor-price-list-title = day name
        //   .elementor-price-list-description = dish text (may contain <br>)
        List<ElementHandle> items = page.querySelectorAll(".elementor-price-list-item");
        if (items.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();

        for (ElementHandle item : items) {
            ElementHandle titleEl = item.querySelector(".elementor-price-list-title");
            ElementHandle descEl = item.querySelector(".elementor-price-list-description");
            if (titleEl == null || descEl == null) continue;

            String titleText = titleEl.innerText().trim();
            DayOfWeek day = DAYS.get(titleText.toLowerCase());
            if (day == null) continue;

            // Description may contain multiple dishes separated by <br>
            String descText = descEl.innerText().trim();
            if (descText.isBlank()) continue;

            List<Dish> dishes = new ArrayList<>();
            for (String line : descText.split("\n")) {
                String trimmed = line.trim();
                if (!trimmed.isBlank()) {
                    dishes.add(new Dish(trimmed));
                }
            }
            if (!dishes.isEmpty()) {
                byDay.put(day, dishes);
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
