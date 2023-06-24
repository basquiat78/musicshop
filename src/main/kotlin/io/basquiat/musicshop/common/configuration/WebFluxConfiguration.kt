package io.basquiat.musicshop.common.configuration

import io.basquiat.musicshop.common.resolver.AuthorizeTokenMethodResolver
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@Configuration
class WebFluxConfiguration(
    private val authorizeTokenMethodResolver: AuthorizeTokenMethodResolver,
) : WebFluxConfigurer {

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        super.configureArgumentResolvers(configurer)
        configurer.addCustomResolver(authorizeTokenMethodResolver)
    }
}