package dev.asyl.portfolio.booking;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {
    @Mock BookingRepository repository;
    @InjectMocks BookingService service;
    private final Instant start=Instant.parse("2026-01-01T10:00:00Z");
    @Test void rejectsOverlappingConfirmedBooking(){
        when(repository.existsByResourceIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                "room-1",BookingStatus.CONFIRMED,start.plusSeconds(3600),start)).thenReturn(true);
        assertThrows(BookingService.BookingConflictException.class,
                ()->service.create("room-1","customer-1",start,start.plusSeconds(3600)));
        verify(repository,never()).save(any());
    }
    @Test void adjacentTimeSlotsDoNotOverlap(){
        when(repository.existsByResourceIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                "room-1",BookingStatus.CONFIRMED,start.plusSeconds(7200),start.plusSeconds(3600))).thenReturn(false);
        service.create("room-1","customer-2",start.plusSeconds(3600),start.plusSeconds(7200));
        verify(repository).save(any(Booking.class));
    }
    @Test void rejectsInvalidRange(){
        assertThrows(IllegalArgumentException.class,()->service.create("room-1","c",start,start));
        verifyNoInteractions(repository);
    }
}
