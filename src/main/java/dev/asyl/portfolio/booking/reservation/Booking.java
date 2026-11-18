package dev.asyl.portfolio.booking.reservation;

import dev.asyl.portfolio.booking.resource.Resource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(name = "customer_name", nullable = false, length = 120)
    private String customerName;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    protected Booking() {
    }

    public Booking(Resource resource,
                   String customerName,
                   Instant startsAt,
                   Instant endsAt,
                   Instant createdAt) {
        this.resource = resource;
        this.customerName = customerName;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.status = BookingStatus.CONFIRMED;
        this.createdAt = createdAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = BookingStatus.CONFIRMED;
        }
    }

    public void cancel(Instant now) {
        if (status == BookingStatus.CANCELLED) {
            return;
        }
        status = BookingStatus.CANCELLED;
        cancelledAt = now;
    }

    public boolean isCancelled() {
        return status == BookingStatus.CANCELLED;
    }

    public Long getId() {
        return id;
    }

    public Resource getResource() {
        return resource;
    }

    public String getCustomerName() {
        return customerName;
    }

    public Instant getStartsAt() {
        return startsAt;
    }

    public Instant getEndsAt() {
        return endsAt;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }
}
