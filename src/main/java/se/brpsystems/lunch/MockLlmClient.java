package se.brpsystems.lunch;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MockLlmClient implements MenuExtractor {

    private static final List<String> SAMPLE_MENUS = List.of(
            "Pasta Carbonara - 105 kr\nKyckling med rotfrukter och rosmarin - 110 kr\nVegetarisk lasagne - 95 kr",
            "Röd linssoppa med naan - 85 kr\nGrillad lax med potatispuré och kaprissmör - 125 kr\nCaesar-sallad med parmesan - 95 kr",
            "Köttbullar med gräddsås, lingon och pressgurka - 105 kr\nUgnsrostad kyckling med hasselbackspotatis - 115 kr\nVegansk böjdel med couscous och hummus - 99 kr"
    );

    private static final Map<String, String> FIXED_MENUS = Map.of(
            "Grand",
            "Entrecôte med bearnaisesås och pommes frites - 145 kr\nVegetarisk Wellington med rotsaker - 125 kr\nFisksoppa med aioli och baguette - 115 kr",

            "Yogi",
            "Dal makhani (vegansk) - 99 kr\nChicken tikka masala med ris och naan - 115 kr\nAloo gobi (vegansk) - 95 kr"
    );

    @Override
    public String extractMenu(String restaurantName, String pageContent, LocalDate date) {
        System.out.println("  [MOCK] Returning fake menu for " + restaurantName + " (" + date + ")");
        if (FIXED_MENUS.containsKey(restaurantName)) {
            return FIXED_MENUS.get(restaurantName);
        }
        return SAMPLE_MENUS.get(new Random().nextInt(SAMPLE_MENUS.size()));
    }
}
