package se.brpsystems.lunch;

public record LunchResult(Restaurant restaurant, String menu, String error) {
    public boolean success() {
        return error == null;
    }
}
