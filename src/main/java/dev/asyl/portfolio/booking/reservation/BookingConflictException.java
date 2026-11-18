package dev.asyl.portfolio.booking.reservation;

import java.time.Instant;

public class BookingConflictException extends RuntimeException {

    public BookingConflictException(Long resourceId, Instant startsAt, Instant endsAt) {
        super("The requested interval [%s, %s) overlaps an existing booking for resource %d"
                .formatted(startsAt, endsAt, resourceId));
    }
}
