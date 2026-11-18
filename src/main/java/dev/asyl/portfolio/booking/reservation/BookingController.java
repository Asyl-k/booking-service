package dev.asyl.portfolio.booking.reservation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class BookingController {

    private final BookingService service;

    public BookingController(BookingService service) {
        this.service = service;
    }

    @PostMapping("/bookings")
    public ResponseEntity<BookingDtos.BookingResponse> create(
            @Valid @RequestBody BookingDtos.CreateBookingRequest request
    ) {
        BookingDtos.BookingResponse created = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/bookings/{id}")
    public BookingDtos.BookingResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/bookings/{id}/cancel")
    public BookingDtos.BookingResponse cancel(@PathVariable Long id) {
        return service.cancel(id);
    }

    @GetMapping("/resources/{resourceId}/bookings")
    public Page<BookingDtos.BookingResponse> listForResource(
            @PathVariable Long resourceId,
            @RequestParam(required = false) BookingStatus status,
            @PageableDefault(size = 20, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return service.listForResource(resourceId, status, pageable);
    }
}
