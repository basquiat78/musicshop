package io.basquiat.musicshop.common.security.filter

import io.basquiat.musicshop.common.security.covert.JwtAuthenticationConverter
import io.basquiat.musicshop.common.security.manager.CustomAuthenticationManager
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.stereotype.Component

@Component
class CustomAuthenticationWebFilter(
    private val jwtAuthenticationConverter: JwtAuthenticationConverter,
    private val customAuthenticationManager: CustomAuthenticationManager,
) {

    fun authenticationWebFilter(): AuthenticationWebFilter {
        val authenticationWebFilter = AuthenticationWebFilter(customAuthenticationManager)
        authenticationWebFilter.setServerAuthenticationConverter(jwtAuthenticationConverter)
        return authenticationWebFilter
    }
}