package com.example.bankcards.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bankCardsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Bank Cards API")
                        .description("REST API for managing bank cards")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("A.V_Team")
                                .email("a.v@test.ru"))
                );
    }
}
