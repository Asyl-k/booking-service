package dev.asyl.portfolio.booking.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    ResourceRepository resources;

    ResourceService service;

    @BeforeEach
    void setUp() {
        service = new ResourceService(resources, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createTrimsNameAndUsesClock() {
        when(resources.save(any(Resource.class))).thenAnswer(invocation -> {
            Resource resource = invocation.getArgument(0);
            ReflectionTestUtils.setField(resource, "id", 10L);
            return resource;
        });

        ResourceDtos.ResourceResponse response =
                service.create(new ResourceDtos.CreateResourceRequest("  Room A  "));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.name()).isEqualTo("Room A");
        assertThat(response.createdAt()).isEqualTo(NOW);
    }

    @Test
    void getUnknownResourceIsNotFound() {
        when(resources.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void listReturnsMappedPage() {
        Resource resource = new Resource("Room A", NOW);
        ReflectionTestUtils.setField(resource, "id", 10L);
        Pageable pageable = PageRequest.of(0, 20);
        when(resources.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(resource), pageable, 1));

        assertThat(service.list(pageable).getContent())
                .extracting(ResourceDtos.ResourceResponse::name)
                .containsExactly("Room A");
    }

    @Test
    void requireReturnsExistingResource() {
        Resource resource = new Resource("Room A", NOW);
        when(resources.findById(10L)).thenReturn(Optional.of(resource));

        assertThat(service.require(10L)).isSameAs(resource);
        verify(resources).findById(10L);
    }
}
