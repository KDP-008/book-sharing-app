package com.bsa.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenApiConfigTest {

    @Test
    void customOpenAPI_shouldReturnConfiguredApiMetadata() {
        OpenApiConfig config = new OpenApiConfig();

        OpenAPI openAPI = config.customOpenAPI();

        assertNotNull(openAPI);
        assertNotNull(openAPI.getInfo());
        assertEquals("Book Sharing API", openAPI.getInfo().getTitle());
        assertEquals("v1", openAPI.getInfo().getVersion());
        assertEquals("APIs for the Book Sharing application used by the UI and clients.", openAPI.getInfo().getDescription());
        assertNotNull(openAPI.getInfo().getContact());
        assertEquals("Dev Team", openAPI.getInfo().getContact().getName());
        assertEquals("dev@example.com", openAPI.getInfo().getContact().getEmail());
    }
}
