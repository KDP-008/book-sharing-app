package com.kdp.app.config;

import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Book Sharing API")
                        .version("v1")
                        .description("APIs for the Book Sharing application used by the UI and clients.")
                        .contact(new Contact().name("Dev Team").email("dev@example.com"))
                );
    }
}
