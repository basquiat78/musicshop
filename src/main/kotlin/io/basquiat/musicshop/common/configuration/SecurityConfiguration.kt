package io.basquiat.musicshop.common.configuration

import io.basquiat.musicshop.common.security.handler.CustomAccessDeniedHandler
import io.basquiat.musicshop.common.security.handler.CustomAuthenticationEntryPoint
import io.basquiat.musicshop.common.security.manager.CustomAuthenticationManager
import io.basquiat.musicshop.common.security.repository.CustomSecurityContextRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration(
    private val authenticationEntryPoint: CustomAuthenticationEntryPoint,
    private val customAccessDeniedHandler: CustomAccessDeniedHandler,
    private val customSecurityContextRepository: CustomSecurityContextRepository,
    private val authenticationManager: CustomAuthenticationManager,
) {

    private val noAuthUri = arrayOf(
        "/api/v1/members/signin",
        "/api/v1/members/signup",
        "/musicshop",
        "/api-docs",
        "/webjars/swagger-ui/index.html",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-resources/**",
        "/swagger-resources",
        "/v3/api-docs/**",
        "/proxy/**",
        "/webjars/**"
    )


    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http.cors { it.disable() }
                   .httpBasic { it.disable() }
                   .csrf { it.disable() }
                   .formLogin { it.disable() }
                   .logout { it.disable() }
                   .authorizeExchange { exchanges ->
                        exchanges.pathMatchers(*noAuthUri).permitAll()
                                 .anyExchange()
                                 .authenticated()
                   }
                   .exceptionHandling {
                       it.authenticationEntryPoint(authenticationEntryPoint)
                       it.accessDeniedHandler(customAccessDeniedHandler)
                   }
                  .securityContextRepository(customSecurityContextRepository)
                  .authenticationManager(authenticationManager)
                  .build()
    }

}