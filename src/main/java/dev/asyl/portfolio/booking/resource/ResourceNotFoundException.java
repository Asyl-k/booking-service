package dev.asyl.portfolio.booking.resource;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Long id) {
        super("Resource %d not found".formatted(id));
    }
}
