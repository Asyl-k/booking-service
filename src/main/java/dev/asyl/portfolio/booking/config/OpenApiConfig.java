package dev.asyl.portfolio.booking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Booking API")
                        .version("1.0.0")
                        .description("Resource bookings protected by a PostgreSQL range exclusion constraint"));
    }
}
