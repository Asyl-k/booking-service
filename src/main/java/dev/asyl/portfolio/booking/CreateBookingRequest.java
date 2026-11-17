package dev.asyl.portfolio.booking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
public record CreateBookingRequest(@NotBlank String resourceId,@NotBlank String customerId,@NotNull Instant startsAt,@NotNull Instant endsAt){}
