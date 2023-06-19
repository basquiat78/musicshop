package io.basquiat.musicshop.common.advice

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.exception.MissingInformationException
import io.basquiat.musicshop.common.exception.NotFoundException
import io.basquiat.musicshop.common.model.response.ApiError
import org.springframework.beans.TypeMismatchException
import org.springframework.core.codec.DecodingException
import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import reactor.core.publisher.Mono
import java.time.LocalDateTime.now
import java.util.regex.Matcher
import java.util.regex.Pattern

@RestControllerAdvice
class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException::class)
    fun handleNotFoundException(ex: NotFoundException): Mono<ApiError> {
        return Mono.just(ApiError(
            code = HttpStatus.NOT_FOUND.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(MissingInformationException::class)
    fun handleMissingInformationException(ex: MissingInformationException): Mono<ApiError> {
        return Mono.just(ApiError(
            code = HttpStatus.NOT_FOUND.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(BadParameterException::class)
    fun handleBadParameterException(ex: BadParameterException): Mono<ApiError> {
        return Mono.just(ApiError(
            code = HttpStatus.NOT_FOUND.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(ex: WebExchangeBindException): Mono<ApiError> {
        val errors = ex.bindingResult.allErrors.first()
        return Mono.just(ApiError(
            code = HttpStatus.BAD_REQUEST.value(),
            message = errors.defaultMessage!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DecodingException::class)
    fun handleJsonParserErrors(ex: DecodingException): Mono<ApiError> {
        val enumMessage = Pattern.compile("not one of the values accepted for Enum class: \\[([^\\]]+)]")
        if (ex.cause != null && ex.cause is InvalidFormatException) {
            val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
            if (matcher.find()) {
                return Mono.just(ApiError(
                    code = HttpStatus.BAD_REQUEST.value(),
                    message = "enum value should be: " + matcher.group(1),
                    timestamp = now(),
                ))
            }
        }
        return Mono.just(ApiError(
            code = HttpStatus.BAD_REQUEST.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(TypeMismatchException::class)
    fun handleValidationExceptions(ex: TypeMismatchException): Mono<ApiError> {
        val enumMessage = Pattern.compile(".*Sort.*")
        if (ex.cause != null && ex.cause is ConversionFailedException) {
            val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
            if (matcher.matches()) {
                return Mono.just(ApiError(
                    code = HttpStatus.BAD_REQUEST.value(),
                    message = "Sort Direction should be: [DESC, ASC]",
                    timestamp = now(),
                ))
            }
        }
        return Mono.just(ApiError(
            code = HttpStatus.BAD_REQUEST.value(),
            message = ex.message!!,
            timestamp = now(),
        ))
    }

}