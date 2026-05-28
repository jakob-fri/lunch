package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

import java.util.List;

public class BrasseritScraper extends BaseMenuScraper {

    @Override
    protected WeeklyMenu extract(Page page) {
        // Brasserit (Brasserie Bouquet): all-week menu listed as h3 in .el-content
        // The menu is under #page\#0 .el-content h3
        List<Dish> dishes = dishesOf(page, ".el-content h3");

        if (!dishes.isEmpty()) {
            return WeeklyMenu.allWeek(dishes);
        }

        // Fallback: try all h3 tags in main content
        List<Dish> fallback = dishesOf(page, "#tm-main h3");
        return fallback.isEmpty() ? WeeklyMenu.empty() : WeeklyMenu.allWeek(fallback);
    }
}
