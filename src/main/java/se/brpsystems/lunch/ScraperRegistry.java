package se.brpsystems.lunch;

import java.util.Map;

public class ScraperRegistry {

    private static final Map<String, MenuScraper> SCRAPERS = Map.ofEntries(
            Map.entry("Grand",         new GrandScraper()),
            Map.entry("Yogi",          new YogiScraper()),
            Map.entry("Brasserit",     new BrasseritScraper()),
            Map.entry("Von dufva",     new VonDufvaScraper()),
            Map.entry("Cioccolata",    new CioccolataScraper()),
            Map.entry("Claras coffee", new ClarasCoffeeScraper()),
            Map.entry("Ekkällan",      new EkkallanScraper()),
            Map.entry("Husman",        new HusmanScraper()),
            Map.entry("Chili Lime",    new ChiliLimeScraper()),
            Map.entry("Stångs",        new StangsScraper()),
            Map.entry("Tropikhuset",   new TropikhusetScraper()),
            Map.entry("Ekoxen",        new EkoxenScraper()),
            Map.entry("ÖK Smak & Tak",  new OkSmakOchTakScraper()),
            Map.entry("Bistro Cô Minh", new BistroCominhScraper())
    );

    private static final MenuScraper DEFAULT = new DefaultMenuScraper();

    public static MenuScraper get(String restaurantName) {
        return SCRAPERS.getOrDefault(restaurantName, DEFAULT);
    }
}
