package io.basquiat.musicshop.common.resolver

import io.basquiat.musicshop.common.aop.AuthorizeToken
import io.basquiat.musicshop.common.exception.BadAuthorizeTokenException
import io.basquiat.musicshop.common.properties.JwtProperties
import io.basquiat.musicshop.common.utils.extractToken
import org.springframework.core.MethodParameter
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class AuthorizeTokenMethodResolver(
    private val props: JwtProperties,
): HandlerMethodArgumentResolver {
    /** 컨트롤러의 파라미터에서 해당 어노테이션이 있는지 확인한다. */
    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.hasParameterAnnotation(AuthorizeToken::class.java)
    }

    /** 만일 있다면 어노테이션이 붙어 있는 파라미터에 값을 바인드 하는 역할을 하게 된다. */
    override fun resolveArgument(
        parameter: MethodParameter,
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any> {
        val bearerToken = exchange.request.headers["Authorization"]?.first() ?: throw BadAuthorizeTokenException("헤더에 Authorization 정보가 존재하지 않습니다.")
        return extractToken(bearerToken, props).toMono()
    }

}