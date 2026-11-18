package dev.asyl.portfolio.booking.reservation;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long id) {
        super("Booking %d not found".formatted(id));
    }
}
