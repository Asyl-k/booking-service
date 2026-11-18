package dev.asyl.portfolio.booking.reservation;

import dev.asyl.portfolio.booking.resource.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class BookingTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void newBookingIsConfirmed() {
        Booking booking = booking();

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(booking.isCancelled()).isFalse();
        assertThat(booking.getCancelledAt()).isNull();
    }

    @Test
    void cancelMarksBookingAndRecordsTime() {
        Booking booking = booking();

        booking.cancel(NOW.plusSeconds(60));

        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(booking.isCancelled()).isTrue();
        assertThat(booking.getCancelledAt()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    void repeatedCancelKeepsFirstCancellationTime() {
        Booking booking = booking();
        booking.cancel(NOW.plusSeconds(60));

        booking.cancel(NOW.plusSeconds(600));

        assertThat(booking.getCancelledAt()).isEqualTo(NOW.plusSeconds(60));
    }

    private Booking booking() {
        Resource resource = new Resource("Room A", NOW);
        ReflectionTestUtils.setField(resource, "id", 1L);
        return new Booking(
                resource,
                "Alice",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                NOW
        );
    }
}
