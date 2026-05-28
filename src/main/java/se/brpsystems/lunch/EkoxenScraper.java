package se.brpsystems.lunch;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Page;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EkoxenScraper extends BaseMenuScraper {

    private static final Map<String, DayOfWeek> DAYS = Map.of(
            "måndag", DayOfWeek.MONDAY,
            "tisdag", DayOfWeek.TUESDAY,
            "onsdag", DayOfWeek.WEDNESDAY,
            "torsdag", DayOfWeek.THURSDAY,
            "fredag", DayOfWeek.FRIDAY
    );

    @Override
    protected WeeklyMenu extract(Page page) {
        // Ekoxen (kvartersmenyn.se): single .meny div containing all days.
        // Format: <strong>Måndag</strong><br>dish1<br>dish2<br><br><strong>Tisdag</strong>...
        // innerText produces newlines at <br>, so we split on lines and detect day headers.
        ElementHandle menyEl = page.querySelector(".meny");
        if (menyEl == null) {
            return WeeklyMenu.empty();
        }

        String text = menyEl.innerText();
        if (text == null || text.isBlank()) {
            return WeeklyMenu.empty();
        }

        Map<DayOfWeek, List<Dish>> byDay = new LinkedHashMap<>();
        DayOfWeek current = null;

        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;

            DayOfWeek day = DAYS.get(trimmed.toLowerCase());
            if (day != null) {
                current = day;
                byDay.putIfAbsent(current, new ArrayList<>());
            } else if (current != null && !isNoise(trimmed)) {
                byDay.computeIfAbsent(current, k -> new ArrayList<>()).add(new Dish(trimmed));
            }
        }

        byDay.values().removeIf(List::isEmpty);
        return byDay.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.perDay(byDay);
    }

    private boolean isNoise(String text) {
        String lower = text.toLowerCase();
        // Skip pricing/info lines that appear after the last day
        return lower.startsWith("pris:")
                || lower.startsWith("inkl.")
                || lower.startsWith("öppet:")
                || lower.startsWith("lunch:")
                || lower.startsWith("middag:");
    }
}
