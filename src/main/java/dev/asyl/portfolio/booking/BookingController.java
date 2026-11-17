package dev.asyl.portfolio.booking;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {
    private final BookingService service;
    public BookingController(BookingService service){this.service=service;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody CreateBookingRequest request){
        return service.create(request.resourceId(),request.customerId(),request.startsAt(),request.endsAt());
    }
    @PostMapping("/{id}/cancel") public Booking cancel(@PathVariable Long id){return service.cancel(id);}
}
