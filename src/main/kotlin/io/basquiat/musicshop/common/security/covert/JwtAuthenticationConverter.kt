package io.basquiat.musicshop.common.security.covert

import io.basquiat.musicshop.common.properties.JwtProperties
import io.basquiat.musicshop.common.utils.extractToken
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class JwtAuthenticationConverter(
    private val props: JwtProperties,
): ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> = mono {
        exchange.request.headers["Authorization"]?.first()?.let { bearerToken ->
            val authToken = extractToken(bearerToken, props)
            UsernamePasswordAuthenticationToken(authToken, authToken)
        }.toMono().awaitSingle()
    }

}