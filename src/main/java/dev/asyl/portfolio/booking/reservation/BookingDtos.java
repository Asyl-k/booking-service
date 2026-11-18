package dev.asyl.portfolio.booking.reservation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class BookingDtos {

    private BookingDtos() {
    }

    public record CreateBookingRequest(
            @NotNull Long resourceId,
            @NotBlank @Size(max = 120) String customerName,
            @NotNull Instant startsAt,
            @NotNull Instant endsAt
    ) {
    }

    public record BookingResponse(
            Long id,
            Long resourceId,
            String customerName,
            Instant startsAt,
            Instant endsAt,
            BookingStatus status,
            Instant createdAt,
            Instant cancelledAt
    ) {

        public static BookingResponse from(Booking booking) {
            return new BookingResponse(
                    booking.getId(),
                    booking.getResource().getId(),
                    booking.getCustomerName(),
                    booking.getStartsAt(),
                    booking.getEndsAt(),
                    booking.getStatus(),
                    booking.getCreatedAt(),
                    booking.getCancelledAt()
            );
        }
    }
}
