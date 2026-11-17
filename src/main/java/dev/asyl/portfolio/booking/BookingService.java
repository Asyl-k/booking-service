package dev.asyl.portfolio.booking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
@Transactional
public class BookingService {
    private final BookingRepository repository;
    public BookingService(BookingRepository repository){this.repository=repository;}
    public Booking create(String resourceId,String customerId,Instant startsAt,Instant endsAt){
        if(!endsAt.isAfter(startsAt))throw new IllegalArgumentException("End must be after start");
        boolean conflict=repository.existsByResourceIdAndStatusAndStartsAtLessThanAndEndsAtGreaterThan(
                resourceId,BookingStatus.CONFIRMED,endsAt,startsAt);
        if(conflict)throw new BookingConflictException(resourceId);
        return repository.save(new Booking(resourceId,customerId,startsAt,endsAt));
    }
    public Booking cancel(Long id){
        Booking booking=repository.findById(id).orElseThrow(()->new BookingNotFoundException(id));
        booking.cancel();
        return repository.save(booking);
    }
    public static class BookingConflictException extends RuntimeException{
        public BookingConflictException(String resource){super("Resource is already booked: "+resource);}
    }
    public static class BookingNotFoundException extends RuntimeException{
        public BookingNotFoundException(Long id){super("Booking %d not found".formatted(id));}
    }
}
