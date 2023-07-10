package io.basquiat.musicshop.common.security.handler

import io.basquiat.musicshop.common.model.response.ApiError
import io.basquiat.musicshop.common.utils.toByte
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime.now

@Component
class CustomAuthenticationEntryPoint: ServerAuthenticationEntryPoint {

    override fun commence(exchange: ServerWebExchange, ex: AuthenticationException): Mono<Void> = mono {
        with(exchange.response) {
            statusCode = HttpStatus.UNAUTHORIZED
            headers.contentType = MediaType.APPLICATION_JSON
            val error = ApiError(
                code = HttpStatus.UNAUTHORIZED.value(),
                message = ex.message!!,
                timestamp = now(),
            )
            val buffer = bufferFactory().wrap(toByte(error))
            writeWith(buffer.toMono()).awaitSingle()
        }
    }
}