package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StangsScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    protected WeeklyMenu extract(Page page) {
        // Stångs: Angular app with .day containers
        // .dayname = day name, .productname = main dish, .dailygreenhead = vegetarian label,
        // .dailygreenfood = vegetarian dish
        List<ElementHandle> dayEls = page.querySelectorAll(".day");
        if (dayEls.isEmpty()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();

        for (ElementHandle dayEl : dayEls) {
            ElementHandle dayNameEl = dayEl.querySelector(".dayname");
            if (dayNameEl == null) continue;
            String dayName = dayNameEl.innerText().trim().toLowerCase();
            DayOfWeek day = DAYS.get(dayName);
            if (day == null) continue;

            List<Dish> dishes = new ArrayList<>();

            // Main dish
            ElementHandle productNameEl = dayEl.querySelector(".productname");
            if (productNameEl != null) {
                String name = productNameEl.innerText().trim();
                if (!name.isBlank()) {
                    // Try to include description too
                    ElementHandle descrEl = dayEl.querySelector(".productdescr");
                    String full = descrEl != null && !descrEl.innerText().trim().isBlank()
                            ? name + " - " + descrEl.innerText().trim()
                            : name;
                    dishes.add(new Dish(full));
                }
            }

            // Vegetarian/green dish
            ElementHandle greenHeadEl = dayEl.querySelector(".dailygreenhead");
            ElementHandle greenFoodEl = dayEl.querySelector(".dailygreenfood");
            if (greenHeadEl != null && greenFoodEl != null) {
                String greenName = greenHeadEl.innerText().trim();
                String greenFood = greenFoodEl.innerText().trim();
                if (!greenName.isBlank() && !greenFood.isBlank()) {
                    dishes.add(new Dish(greenName + " - " + greenFood));
                }
            }

            if (!dishes.isEmpty()) {
                byDay.put(day, dishes);
            }
        }

        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }
}
