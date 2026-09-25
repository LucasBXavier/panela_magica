package com.panelamagica.panelamagica.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.extern.java.Log;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Log
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("API Panela Mágica")
                                .version("v1")
                                .description("Documentação da API de receitas culinárias do Panela Mágica"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    @Bean
    public ApplicationListener<ApplicationReadyEvent> swaggerStartupLogger(Environment environment) {

        return event -> {
            String port = environment.getProperty("server.port", "8080");

            String contextPath = environment.getProperty("server.servlet.context-path", "");

            String swaggerPath = environment.getProperty("springdoc.swagger-ui.path", "/swagger-ui.html");

            String swaggerUrl = "http://localhost:" + port + contextPath + swaggerPath;

            log.info("🚀 Swagger UI disponível em:");
            log.info("👉 " + swaggerUrl);
        };
    }
}
