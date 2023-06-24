package io.basquiat.musicshop.common.configuration

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfiguration {

    @Bean
    fun groupedOpenApi(): GroupedOpenApi {
        return GroupedOpenApi.builder()
                             .group("musicshop")
                             .pathsToMatch("/api/**")
                             .build()
    }

    @Bean
    fun openAPI(): OpenAPI {
        val securitySchemeName = "bearerAuth"
        return OpenAPI().addSecurityItem(SecurityRequirement().addList(securitySchemeName))
                        .components(
                            Components().addSecuritySchemes(securitySchemeName, SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT"))
                        )
                        .info(
                            Info().title("MusicShop with WebFlux API")
                                  .description("웹플럭스로 만든 뮤직샵 API")
                                  .version("v3")
                        )
    }

}