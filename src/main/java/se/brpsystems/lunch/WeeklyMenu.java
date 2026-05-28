package se.brpsystems.lunch;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public record WeeklyMenu(
        Map<DayOfWeek, List<Dish>> byDay,
        List<Dish> allWeek) {

    public static WeeklyMenu perDay(Map<DayOfWeek, List<Dish>> byDay) {
        return new WeeklyMenu(Map.copyOf(byDay), List.of());
    }

    public static WeeklyMenu allWeek(List<Dish> items) {
        return new WeeklyMenu(Map.of(), List.copyOf(items));
    }

    public static WeeklyMenu empty() {
        return new WeeklyMenu(Map.of(), List.of());
    }

    public boolean isEmpty() {
        return byDay.isEmpty() && allWeek.isEmpty();
    }

    /**
     * Per-day sites: returns that day's list (empty if day not found).
     * All-week sites: returns the shared list regardless of day.
     */
    public List<Dish> forDay(DayOfWeek day) {
        if (!byDay.isEmpty()) {
            return byDay.getOrDefault(day, List.of());
        }
        return allWeek;
    }
}
