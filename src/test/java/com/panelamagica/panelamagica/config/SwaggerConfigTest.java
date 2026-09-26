package com.panelamagica.panelamagica.config;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SwaggerConfigTest {

    private final SwaggerConfig config = new SwaggerConfig();

    @Test
    void openApiTemEsquemaBearer() {
        OpenAPI api = config.customOpenAPI();
        assertThat(api.getInfo().getTitle()).isEqualTo("API Panela Mágica");
        assertThat(api.getComponents().getSecuritySchemes()).containsKey("bearerAuth");
    }

    @Test
    void listenerExecutaComPadroesEComPropriedades() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        config.swaggerStartupLogger(new MockEnvironment()).onApplicationEvent(event);

        MockEnvironment env = new MockEnvironment()
                .withProperty("server.port", "9090")
                .withProperty("server.servlet.context-path", "/x")
                .withProperty("springdoc.swagger-ui.path", "/ui");
        config.swaggerStartupLogger(env).onApplicationEvent(event);
    }
}
