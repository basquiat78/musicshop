package io.basquiat.musicshop.common.advice

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.exception.MissingInformationException
import io.basquiat.musicshop.common.exception.NotFoundException
import io.basquiat.musicshop.common.model.response.ApiError
import io.basquiat.musicshop.common.utils.logger
import org.springframework.boot.autoconfigure.web.WebProperties
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler
import org.springframework.boot.web.reactive.error.ErrorAttributes
import org.springframework.context.ApplicationContext
import org.springframework.core.annotation.Order
import org.springframework.core.codec.DecodingException
import org.springframework.http.HttpStatus
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.server.*
import org.springframework.web.server.ServerWebInputException
import java.util.regex.Matcher
import java.util.regex.Pattern

@Component
@Order(-2)
class GlobalExceptionHandler(
    errorAttributes: ErrorAttributes,
    serverCodecConfigurer: ServerCodecConfigurer,
    applicationContext: ApplicationContext,
): AbstractErrorWebExceptionHandler(errorAttributes, WebProperties.Resources(), applicationContext) {

    init {
        super.setMessageWriters(serverCodecConfigurer.writers)
        super.setMessageReaders(serverCodecConfigurer.readers)
    }

    private val log = logger<GlobalExceptionHandler>()

    override fun getRoutingFunction(errorAttributes: ErrorAttributes): RouterFunction<ServerResponse> {
        return RouterFunctions.route(RequestPredicates.all(), rendering)
    }

    val rendering = HandlerFunction { request ->
        when (val ex = super.getError(request)) {
            is NotFoundException, is MissingInformationException -> {
                log.error(ex.message)
                ServerResponse.status(HttpStatus.NOT_FOUND)
                              .bodyValue(ApiError(code = HttpStatus.NOT_FOUND.value(), message = ex.message!!))
            }
            is BadParameterException -> {
                log.error(ex.message)
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                              .bodyValue(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = ex.message!!))
            }
            is ServerWebInputException -> {
                log.error(ex.message)
                val enumMessage = Pattern.compile("not one of the values accepted for Enum class: \\[([^\\]]+)]")
                if (ex.cause != null && ex.cause is DecodingException) {
                    val matcher: Matcher = enumMessage.matcher(ex.cause!!.message)
                    if (matcher.find()) {
                        return@HandlerFunction ServerResponse.status(HttpStatus.BAD_REQUEST)
                                                             .bodyValue(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = "enum value should be: " + matcher.group(1)))
                    }
                }
                ServerResponse.status(HttpStatus.BAD_REQUEST)
                              .bodyValue(ApiError(code = HttpStatus.BAD_REQUEST.value(), message = ex.message))
            }
            else -> {
                log.error(ex.message, ex)
                ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                              .bodyValue(ApiError(code = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = ex.message!!))
            }
        }
    }

}