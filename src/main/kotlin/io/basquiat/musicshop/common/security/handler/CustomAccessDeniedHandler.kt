package io.basquiat.musicshop.common.security.handler

import io.basquiat.musicshop.common.model.response.ApiError
import io.basquiat.musicshop.common.utils.toByte
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime.now

@Component
class CustomAccessDeniedHandler: ServerAccessDeniedHandler {
    override fun handle(exchange: ServerWebExchange, denied: AccessDeniedException): Mono<Void> = mono {
        with(exchange.response) {
            statusCode = HttpStatus.FORBIDDEN
            headers.contentType = MediaType.APPLICATION_JSON
            val error = ApiError(
                code = HttpStatus.FORBIDDEN.value(),
                message = denied.message!!,
                timestamp = now(),
            )
            val buffer = bufferFactory().wrap(toByte(error))
            writeWith(buffer.toMono()).awaitSingle()
        }
    }
}