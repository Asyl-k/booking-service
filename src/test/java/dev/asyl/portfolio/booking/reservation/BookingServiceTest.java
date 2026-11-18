package dev.asyl.portfolio.booking.reservation;

import dev.asyl.portfolio.booking.resource.Resource;
import dev.asyl.portfolio.booking.resource.ResourceNotFoundException;
import dev.asyl.portfolio.booking.resource.ResourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    BookingRepository bookings;

    @Mock
    ResourceRepository resources;

    BookingService service;

    @BeforeEach
    void setUp() {
        service = new BookingService(bookings, resources, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsFutureBooking() {
        Resource resource = resource(10L);
        when(resources.findById(10L)).thenReturn(Optional.of(resource));
        when(bookings.saveAndFlush(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            ReflectionTestUtils.setField(booking, "id", 100L);
            return booking;
        });

        BookingDtos.BookingResponse response = service.create(new BookingDtos.CreateBookingRequest(
                10L,
                "  Alice  ",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200)
        ));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.resourceId()).isEqualTo(10L);
        assertThat(response.customerName()).isEqualTo("Alice");
        assertThat(response.status()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsIntervalThatEndsBeforeItStarts() {
        assertThatThrownBy(() -> service.create(new BookingDtos.CreateBookingRequest(
                10L,
                "Alice",
                NOW.plusSeconds(7200),
                NOW.plusSeconds(3600)
        ))).isInstanceOf(InvalidBookingIntervalException.class);
        verifyNoInteractions(bookings, resources);
    }

    @Test
    void rejectsZeroLengthInterval() {
        assertThatThrownBy(() -> service.create(new BookingDtos.CreateBookingRequest(
                10L,
                "Alice",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(3600)
        ))).isInstanceOf(InvalidBookingIntervalException.class);
        verifyNoInteractions(bookings, resources);
    }

    @Test
    void rejectsStartInThePast() {
        assertThatThrownBy(() -> service.create(new BookingDtos.CreateBookingRequest(
                10L,
                "Alice",
                NOW.minusSeconds(1),
                NOW.plusSeconds(3600)
        ))).isInstanceOf(StartsAtInPastException.class);
        verifyNoInteractions(bookings, resources);
    }

    @Test
    void rejectsStartEqualToNow() {
        assertThatThrownBy(() -> service.create(new BookingDtos.CreateBookingRequest(
                10L,
                "Alice",
                NOW,
                NOW.plusSeconds(3600)
        ))).isInstanceOf(StartsAtInPastException.class);
        verifyNoInteractions(bookings, resources);
    }

    @Test
    void unknownResourceIsNotFound() {
        when(resources.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new BookingDtos.CreateBookingRequest(
                99L,
                "Alice",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200)
        ))).isInstanceOf(ResourceNotFoundException.class);
        verify(bookings, never()).saveAndFlush(any(Booking.class));
    }

    @Test
    void exclusionConstraintViolationBecomesConflict() {
        when(resources.findById(10L)).thenReturn(Optional.of(resource(10L)));
        when(bookings.saveAndFlush(any(Booking.class)))
                .thenThrow(new DataIntegrityViolationException("conflicting key value"));

        assertThatThrownBy(() -> service.create(new BookingDtos.CreateBookingRequest(
                10L,
                "Alice",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200)
        ))).isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("overlaps an existing booking");
    }

    @Test
    void cancelUsesPessimisticLock() {
        Booking booking = booking(100L, resource(10L));
        when(bookings.findByIdForUpdate(100L)).thenReturn(Optional.of(booking));

        BookingDtos.BookingResponse response = service.cancel(100L);

        assertThat(response.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(response.cancelledAt()).isEqualTo(NOW);
        verify(bookings).findByIdForUpdate(100L);
    }

    @Test
    void cancellingUnknownBookingIsNotFound() {
        when(bookings.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(99L))
                .isInstanceOf(BookingNotFoundException.class);
    }

    @Test
    void listRequiresExistingResourceAndFiltersByStatus() {
        Resource resource = resource(10L);
        Booking confirmed = booking(100L, resource);
        Pageable pageable = PageRequest.of(0, 20);
        when(resources.existsById(10L)).thenReturn(true);
        when(bookings.findByResourceIdAndStatus(10L, BookingStatus.CONFIRMED, pageable))
                .thenReturn(new PageImpl<>(List.of(confirmed), pageable, 1));

        Page<BookingDtos.BookingResponse> page =
                service.listForResource(10L, BookingStatus.CONFIRMED, pageable);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().id()).isEqualTo(100L);
    }

    @Test
    void listWithoutStatusReturnsAllBookings() {
        Pageable pageable = PageRequest.of(0, 20);
        when(resources.existsById(10L)).thenReturn(true);
        when(bookings.findByResourceId(10L, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.listForResource(10L, null, pageable).getTotalElements()).isZero();
    }

    @Test
    void listForUnknownResourceIsNotFound() {
        when(resources.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.listForResource(99L, null, PageRequest.of(0, 20)))
                .isInstanceOf(ResourceNotFoundException.class);
        verifyNoInteractions(bookings);
    }

    @Test
    void getUnknownBookingIsNotFound() {
        when(bookings.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(BookingNotFoundException.class);
    }

    private Resource resource(Long id) {
        Resource resource = new Resource("Room " + id, NOW);
        ReflectionTestUtils.setField(resource, "id", id);
        return resource;
    }

    private Booking booking(Long id, Resource resource) {
        Booking booking = new Booking(
                resource,
                "Alice",
                NOW.plusSeconds(3600),
                NOW.plusSeconds(7200),
                NOW
        );
        ReflectionTestUtils.setField(booking, "id", id);
        return booking;
    }
}
