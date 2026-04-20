package se.brpsystems.lunch;

import java.time.LocalDate;

public interface MenuExtractor {
    String extractMenu(String restaurantName, String pageContent, LocalDate date) throws Exception;
}
