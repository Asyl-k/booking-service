package dev.asyl.portfolio.booking;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BookingRepository extends JpaRepository<Booking,Long> {
    boolean existsByResourceIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
            String resourceId,BookingStatus status,java.time.Instant requestedEnd,java.time.Instant requestedStart);
}
