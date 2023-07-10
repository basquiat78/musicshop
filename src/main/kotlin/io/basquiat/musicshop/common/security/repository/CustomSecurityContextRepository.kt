package io.basquiat.musicshop.common.security.repository

import io.basquiat.musicshop.common.properties.JwtProperties
import io.basquiat.musicshop.common.utils.extractToken
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextImpl
import org.springframework.security.web.server.context.ServerSecurityContextRepository
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class CustomSecurityContextRepository(
    private val props: JwtProperties,
    private val authenticationManager: ReactiveAuthenticationManager,
): ServerSecurityContextRepository {

    override fun save(exchange: ServerWebExchange, context: SecurityContext): Mono<Void> = mono {
        Mono.empty<Void>().awaitSingle()
    }

    override fun load(exchange: ServerWebExchange): Mono<SecurityContext> = mono {
        exchange.request.headers["Authorization"]?.first()?.let { bearerToken ->
            val token = extractToken(bearerToken, props)
            val auth = UsernamePasswordAuthenticationToken(token, token, null)
            authenticationManager.authenticate(auth)
                                 .toMono()
                                 .map(::SecurityContextImpl).awaitSingle()
        }
    }

}