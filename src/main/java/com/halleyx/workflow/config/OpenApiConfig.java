package com.halleyx.workflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Halleyx Workflow Engine API")
                        .version("1.0.0")
                        .description("Advanced Workflow Engine - Design, Execute & Track workflows with dynamic rule evaluation")
                        .contact(new Contact().name("Halleyx").email("dev@halleyx.com"))
                        .license(new License().name("MIT")));
    }
}
