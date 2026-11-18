package dev.asyl.portfolio.booking.reservation;

import dev.asyl.portfolio.booking.resource.Resource;
import dev.asyl.portfolio.booking.resource.ResourceNotFoundException;
import dev.asyl.portfolio.booking.resource.ResourceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final ResourceRepository resources;
    private final Clock clock;

    public BookingService(BookingRepository bookings, ResourceRepository resources, Clock clock) {
        this.bookings = bookings;
        this.resources = resources;
        this.clock = clock;
    }

    @Transactional
    public BookingDtos.BookingResponse create(BookingDtos.CreateBookingRequest request) {
        Instant now = Instant.now(clock);
        if (!request.endsAt().isAfter(request.startsAt())) {
            throw new InvalidBookingIntervalException();
        }
        if (!request.startsAt().isAfter(now)) {
            throw new StartsAtInPastException();
        }
        Resource resource = resources.findById(request.resourceId())
                .orElseThrow(() -> new ResourceNotFoundException(request.resourceId()));

        Booking booking = new Booking(
                resource,
                request.customerName().trim(),
                request.startsAt(),
                request.endsAt(),
                now
        );
        try {
            booking = bookings.saveAndFlush(booking);
        } catch (DataIntegrityViolationException ex) {
            // The PostgreSQL exclusion constraint is the final arbiter under concurrency.
            throw new BookingConflictException(request.resourceId(), request.startsAt(), request.endsAt());
        }
        return BookingDtos.BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public BookingDtos.BookingResponse get(Long id) {
        return BookingDtos.BookingResponse.from(requireBooking(id));
    }

    @Transactional
    public BookingDtos.BookingResponse cancel(Long id) {
        Booking booking = bookings.findByIdForUpdate(id)
                .orElseThrow(() -> new BookingNotFoundException(id));
        booking.cancel(Instant.now(clock));
        return BookingDtos.BookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public Page<BookingDtos.BookingResponse> listForResource(Long resourceId,
                                                             BookingStatus status,
                                                             Pageable pageable) {
        if (!resources.existsById(resourceId)) {
            throw new ResourceNotFoundException(resourceId);
        }
        Page<Booking> page = status == null
                ? bookings.findByResourceId(resourceId, pageable)
                : bookings.findByResourceIdAndStatus(resourceId, status, pageable);
        return page.map(BookingDtos.BookingResponse::from);
    }

    private Booking requireBooking(Long id) {
        return bookings.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
    }
}
