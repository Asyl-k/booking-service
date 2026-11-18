package dev.asyl.portfolio.booking.reservation;

public class InvalidBookingIntervalException extends RuntimeException {

    public InvalidBookingIntervalException() {
        super("Booking end must be after booking start");
    }
}
