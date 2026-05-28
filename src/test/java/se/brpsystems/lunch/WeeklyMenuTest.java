package se.brpsystems.lunch;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WeeklyMenuTest {

    @Test
    void perDayReturnsCorrectDayDishes() {
        var monday = List.of(new Dish("Pasta"), new Dish("Salad"));
        var menu = WeeklyMenu.perDay(Map.of(DayOfWeek.MONDAY, monday));

        assertEquals(monday, menu.forDay(DayOfWeek.MONDAY));
        assertTrue(menu.forDay(DayOfWeek.TUESDAY).isEmpty());
    }

    @Test
    void allWeekReturnsSameListForAnyDay() {
        var dishes = List.of(new Dish("Pasta"), new Dish("Salad"));
        var menu = WeeklyMenu.allWeek(dishes);

        assertEquals(dishes, menu.forDay(DayOfWeek.MONDAY));
        assertEquals(dishes, menu.forDay(DayOfWeek.FRIDAY));
    }

    @Test
    void emptyIsEmpty() {
        assertTrue(WeeklyMenu.empty().isEmpty());
        assertTrue(WeeklyMenu.empty().forDay(DayOfWeek.MONDAY).isEmpty());
    }

    @Test
    void perDayIsNotEmpty() {
        var menu = WeeklyMenu.perDay(Map.of(DayOfWeek.MONDAY, List.of(new Dish("Soup"))));
        assertFalse(menu.isEmpty());
    }

    @Test
    void allWeekWithItemsIsNotEmpty() {
        assertFalse(WeeklyMenu.allWeek(List.of(new Dish("Soup"))).isEmpty());
    }

    @Test
    void allWeekWithEmptyListIsEmpty() {
        assertTrue(WeeklyMenu.allWeek(List.of()).isEmpty());
    }
}
