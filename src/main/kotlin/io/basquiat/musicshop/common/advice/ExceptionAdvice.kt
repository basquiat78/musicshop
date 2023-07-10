package io.basquiat.musicshop.common.advice

import io.basquiat.musicshop.common.exception.BadAuthorizeTokenException
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.exception.DuplicatedMemberException
import io.basquiat.musicshop.common.exception.NotFoundException
import io.basquiat.musicshop.common.model.response.ApiError
import io.basquiat.musicshop.common.utils.toByte
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.time.LocalDateTime.now

@Component
class ExceptionAdvice: ErrorWebExceptionHandler {

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> = mono {
        val pairs = when (ex) {
            is NotFoundException -> HttpStatus.NOT_FOUND to (ex.message ?: "조회된 정보가 없습니다.")
            is BadParameterException -> HttpStatus.BAD_REQUEST to (ex.message ?: "파라미터 정보가 잘못되었습니다.")
            is DuplicatedMemberException -> HttpStatus.NO_CONTENT to (ex.message ?: "중복된 사용자입니다.")
            is BadAuthorizeTokenException -> HttpStatus.UNAUTHORIZED to (ex.message ?: "Not AuthorizeToken")
            is AuthenticationException -> HttpStatus.UNAUTHORIZED to (ex.message ?: "Not Authenticated")
            else -> HttpStatus.INTERNAL_SERVER_ERROR to "Internal Server Error"
        }

        with(exchange.response) {
            statusCode = pairs.first
            headers.contentType = MediaType.APPLICATION_JSON
            val error = ApiError(code = pairs.first.value(), message = pairs.second, timestamp = now())
            val dataBuffer = bufferFactory().wrap(toByte(error))
            writeWith(dataBuffer.toMono()).awaitSingle()
        }

    }

}