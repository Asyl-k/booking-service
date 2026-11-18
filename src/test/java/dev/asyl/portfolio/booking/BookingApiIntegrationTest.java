package dev.asyl.portfolio.booking;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class BookingApiIntegrationTest extends AbstractIntegrationTest {

    private static final Instant BASE = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.HOURS);

    @Test
    void resourceLifecycleAndValidation() {
        JsonNode created = createResource(uniqueName("Room"));
        long resourceId = created.get("id").asLong();

        assertThat(created.get("name").asText()).startsWith("Room-");
        assertThat(created.get("createdAt").asText()).isNotBlank();

        JsonNode fetched = json(get("/api/resources/" + resourceId));
        assertThat(fetched.get("id").asLong()).isEqualTo(resourceId);

        JsonNode page = json(get("/api/resources?size=100"));
        assertThat(page.get("totalElements").asLong()).isGreaterThanOrEqualTo(1);

        assertProblem(get("/api/resources/999999"), 404, "resource_not_found");
        assertProblem(post("/api/resources", Map.of("name", " ")), 400, "validation_failed");
        assertProblem(post("/api/resources", Map.of("name", "x".repeat(121))), 400, "validation_failed");
    }

    @Test
    void adjacentIntervalsAreAllowed() {
        long resourceId = createResource(uniqueName("Room")).get("id").asLong();
        Instant start = BASE.plus(1, ChronoUnit.DAYS);

        assertThat(createBooking(resourceId, "Alice", start, start.plusSeconds(3600)).getStatusCode().value())
                .isEqualTo(201);
        assertThat(createBooking(resourceId, "Bob", start.plusSeconds(3600), start.plusSeconds(7200))
                .getStatusCode().value()).isEqualTo(201);
        assertThat(createBooking(resourceId, "Carol", start.plusSeconds(7200), start.plusSeconds(10800))
                .getStatusCode().value()).isEqualTo(201);
    }

    @Test
    void overlappingIntervalsAreRejected() {
        long resourceId = createResource(uniqueName("Room")).get("id").asLong();
        Instant start = BASE.plus(2, ChronoUnit.DAYS);

        assertThat(createBooking(resourceId, "Alice", start, start.plusSeconds(7200)).getStatusCode().value())
                .isEqualTo(201);
        assertProblem(createBooking(resourceId, "Bob", start.plusSeconds(1800), start.plusSeconds(5400)),
                409, "booking_conflict");
        assertProblem(createBooking(resourceId, "Carol", start.minusSeconds(1800), start.plusSeconds(1800)),
                409, "booking_conflict");
        assertProblem(createBooking(resourceId, "Dave", start, start.plusSeconds(7200)),
                409, "booking_conflict");
    }

    @Test
    void cancellationFreesTheSlot() {
        long resourceId = createResource(uniqueName("Room")).get("id").asLong();
        Instant start = BASE.plus(3, ChronoUnit.DAYS);

        JsonNode booking = json(createBooking(resourceId, "Alice", start, start.plusSeconds(3600)));
        long bookingId = booking.get("id").asLong();
        assertThat(booking.get("status").asText()).isEqualTo("CONFIRMED");

        assertProblem(createBooking(resourceId, "Bob", start, start.plusSeconds(3600)),
                409, "booking_conflict");

        JsonNode cancelled = json(post("/api/bookings/" + bookingId + "/cancel", Map.of()));
        assertThat(cancelled.get("status").asText()).isEqualTo("CANCELLED");
        assertThat(cancelled.get("cancelledAt").asText()).isNotBlank();

        JsonNode rebooked = json(createBooking(resourceId, "Bob", start, start.plusSeconds(3600)));
        assertThat(rebooked.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(rebooked.get("id").asLong()).isNotEqualTo(bookingId);

        JsonNode cancelledPage = json(get("/api/resources/" + resourceId + "/bookings?status=CANCELLED"));
        assertThat(cancelledPage.get("totalElements").asLong()).isEqualTo(1);
        assertThat(cancelledPage.get("content").get(0).get("id").asLong()).isEqualTo(bookingId);

        JsonNode confirmedPage = json(get("/api/resources/" + resourceId + "/bookings?status=CONFIRMED"));
        assertThat(confirmedPage.get("totalElements").asLong()).isEqualTo(1);
        assertThat(confirmedPage.get("content").get(0).get("id").asLong())
                .isEqualTo(rebooked.get("id").asLong());
    }

    @Test
    void repeatedCancellationIsIdempotent() {
        long resourceId = createResource(uniqueName("Room")).get("id").asLong();
        Instant start = BASE.plus(4, ChronoUnit.DAYS);
        long bookingId = json(createBooking(resourceId, "Alice", start, start.plusSeconds(3600)))
                .get("id").asLong();

        JsonNode first = json(post("/api/bookings/" + bookingId + "/cancel", Map.of()));
        JsonNode second = json(post("/api/bookings/" + bookingId + "/cancel", Map.of()));

        assertThat(first.get("cancelledAt").asText()).isEqualTo(second.get("cancelledAt").asText());
        assertThat(second.get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void invalidRequestsReturnStableProblemCodes() {
        long resourceId = createResource(uniqueName("Room")).get("id").asLong();
        Instant start = BASE.plus(5, ChronoUnit.DAYS);

        assertProblem(createBooking(resourceId, "Alice", start, start), 400, "invalid_interval");
        assertProblem(createBooking(resourceId, "Alice", start, start.minusSeconds(60)),
                400, "invalid_interval");
        assertProblem(createBooking(resourceId, "Alice", Instant.now().minusSeconds(60), start),
                400, "starts_at_in_past");
        assertProblem(createBooking(999999, "Alice", start, start.plusSeconds(3600)),
                404, "resource_not_found");
        assertProblem(post("/api/bookings", Map.of(
                "resourceId", resourceId,
                "customerName", "",
                "startsAt", start,
                "endsAt", start.plusSeconds(3600)
        )), 400, "validation_failed");
        assertProblem(post("/api/bookings", Map.of("resourceId", resourceId)),
                400, "validation_failed");
        assertProblem(get("/api/bookings/999999"), 404, "booking_not_found");
        assertProblem(post("/api/bookings/999999/cancel", Map.of()), 404, "booking_not_found");
        assertProblem(get("/api/resources/999999/bookings"), 404, "resource_not_found");
    }

    @Test
    void differentResourcesCanBeBookedConcurrently() throws Exception {
        List<Long> resourceIds = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            resourceIds.add(createResource(uniqueName("Room")).get("id").asLong());
        }
        Instant start = BASE.plus(6, ChronoUnit.DAYS);

        List<Integer> statuses = runConcurrently(10, index -> createBooking(
                resourceIds.get(index),
                "Customer " + index,
                start,
                start.plusSeconds(3600)
        ).getStatusCode().value());

        assertThat(statuses).allMatch(status -> status == 201);
    }

    @Test
    void concurrentBookingsOfTheSameSlotProduceExactlyOneBooking() throws Exception {
        long resourceId = createResource(uniqueName("Room")).get("id").asLong();
        Instant start = BASE.plus(7, ChronoUnit.DAYS);

        List<Integer> statuses = runConcurrently(10, index -> createBooking(
                resourceId,
                "Customer " + index,
                start,
                start.plusSeconds(3600)
        ).getStatusCode().value());

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(9);

        JsonNode page = json(get("/api/resources/" + resourceId + "/bookings"));
        assertThat(page.get("totalElements").asLong()).isEqualTo(1);
        assertThat(page.get("content").get(0).get("status").asText()).isEqualTo("CONFIRMED");
    }

    private void assertProblem(org.springframework.http.ResponseEntity<String> response,
                               int status,
                               String code) {
        assertThat(response.getStatusCode().value()).isEqualTo(status);
        assertThat(response.getHeaders().getContentType()).isNotNull();
        JsonNode problem = json(response);
        assertThat(problem.get("code").asText()).isEqualTo(code);
        assertThat(problem.get("timestamp").asText()).isNotBlank();
    }

    private <T> List<T> runConcurrently(int tasks, ConcurrentTask<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<CompletableFuture<T>> futures = new ArrayList<>();
            for (int i = 0; i < tasks; i++) {
                int index = i;
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        start.await();
                        return task.run(index);
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }, pool));
            }
            start.countDown();
            List<T> results = new ArrayList<>();
            for (CompletableFuture<T> future : futures) {
                results.add(future.join());
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @FunctionalInterface
    private interface ConcurrentTask<T> {
        T run(int index) throws Exception;
    }
}
