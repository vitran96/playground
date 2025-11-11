package tech.kingoyster.spring_1;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(title = "My API", version = "1.0.0"),
        security = @SecurityRequirement(name = "bearerAuth") // Apply globally if needed
)
@SecurityScheme(
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        name = "bearerAuth", // This name will be referenced in SecurityRequirement
        scheme = "bearer",
        in = SecuritySchemeIn.HEADER
)
public class SpringDocConfig {
    // No additional code needed here for basic setup
}
