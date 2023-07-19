package io.basquiat.musicshop.common.configuration

import io.basquiat.musicshop.common.security.filter.CustomAuthenticationWebFilter
import io.basquiat.musicshop.common.security.handler.CustomAccessDeniedHandler
import io.basquiat.musicshop.common.security.handler.CustomAuthenticationEntryPoint
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration(
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val customAuthenticationWebFilter: CustomAuthenticationWebFilter,
) {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        http.cors { it.disable() }
            .httpBasic { it.disable() }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }

        http.securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/v1/members/logout"))
            .securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/v1/musicians/**"))
            .securityMatcher(PathPatternParserServerWebExchangeMatcher("/api/v1/records/**"))

        http.addFilterAt(customAuthenticationWebFilter.authenticationWebFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(customAccessDeniedHandler)
            }
            .authorizeExchange { exchanges ->
                exchanges.anyExchange()
                         .authenticated()
            }

        return http.build()
    }

}