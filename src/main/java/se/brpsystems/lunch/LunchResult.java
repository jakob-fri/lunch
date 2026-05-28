package se.brpsystems.lunch;

import java.util.List;

public record LunchResult(Restaurant restaurant, List<Dish> dishes, String error) {
    public boolean success() {
        return error == null;
    }
}
