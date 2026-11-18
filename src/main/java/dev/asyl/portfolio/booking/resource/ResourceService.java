package dev.asyl.portfolio.booking.resource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ResourceService {

    private final ResourceRepository resources;
    private final Clock clock;

    public ResourceService(ResourceRepository resources, Clock clock) {
        this.resources = resources;
        this.clock = clock;
    }

    @Transactional
    public ResourceDtos.ResourceResponse create(ResourceDtos.CreateResourceRequest request) {
        Resource resource = new Resource(request.name().trim(), Instant.now(clock));
        return ResourceDtos.ResourceResponse.from(resources.save(resource));
    }

    @Transactional(readOnly = true)
    public ResourceDtos.ResourceResponse get(Long id) {
        return ResourceDtos.ResourceResponse.from(require(id));
    }

    @Transactional(readOnly = true)
    public Page<ResourceDtos.ResourceResponse> list(Pageable pageable) {
        return resources.findAll(pageable).map(ResourceDtos.ResourceResponse::from);
    }

    Resource require(Long id) {
        return resources.findById(id).orElseThrow(() -> new ResourceNotFoundException(id));
    }
}
