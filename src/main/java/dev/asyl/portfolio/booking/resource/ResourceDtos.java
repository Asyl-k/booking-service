package dev.asyl.portfolio.booking.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ResourceDtos {

    private ResourceDtos() {
    }

    public record CreateResourceRequest(
            @NotBlank @Size(max = 120) String name
    ) {
    }

    public record ResourceResponse(Long id, String name, Instant createdAt) {

        public static ResourceResponse from(Resource resource) {
            return new ResourceResponse(resource.getId(), resource.getName(), resource.getCreatedAt());
        }
    }
}
