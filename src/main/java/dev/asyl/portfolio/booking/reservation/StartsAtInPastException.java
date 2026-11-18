package dev.asyl.portfolio.booking.reservation;

public class StartsAtInPastException extends RuntimeException {

    public StartsAtInPastException() {
        super("Booking start must be in the future");
    }
}
