package se.brpsystems.lunch;

import com.microsoft.playwright.Page;

public interface MenuScraper {
    WeeklyMenu scrape(Page page, String url);
}
