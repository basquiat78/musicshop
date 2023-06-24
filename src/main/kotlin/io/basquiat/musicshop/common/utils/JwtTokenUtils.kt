package io.basquiat.musicshop.common.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.TokenExpiredException
import com.auth0.jwt.interfaces.DecodedJWT
import io.basquiat.musicshop.common.exception.BadAuthorizeTokenException
import io.basquiat.musicshop.common.properties.JwtProperties
import io.basquiat.musicshop.domain.member.model.dto.JwtClaim
import java.time.LocalDateTime.now
import java.time.ZoneId.systemDefault
import java.util.Date.from

/**
 * generateAuthToken
 * @param jwtClaim
 * @param props
 * @return String
 */
fun generateAuthToken(jwtClaim: JwtClaim, props: JwtProperties): String {
    val algorithm = Algorithm.HMAC256(props.secret)
    return JWT.create()
              .withIssuer(props.issuer)
              .withSubject(props.subject)
              .withIssuedAt(from(now().atZone(systemDefault()).toInstant()))
              .withExpiresAt(from(now().plusSeconds(props.expiredAt).atZone(systemDefault()).toInstant()))
              .withClaim("memberId", jwtClaim.memberId)
              .withClaim("email", jwtClaim.email)
              .sign(algorithm)
}

/**
 * 토큰으로부터 프리픽스로 붙은 부분을 제거하고 jwt를 추출한다.
 * @param token
 * @return String
 */
fun extractToken(token: String, props: JwtProperties): String {
    return try {
        val prefix = token.split(" ")[0]
        if(props.prefix != prefix) {
            throw BadAuthorizeTokenException()
        }
        token.split(" ")[1]
    } catch (e: Exception) {
        throw BadAuthorizeTokenException("유효한 토큰 타입과 형식이 아닙니다.")
    }
}

/**
 * 토큰 검증 및 DecodedJWT반환
 * @param token
 * @param props
 * @return DecodedJWT
 * @throws BadAuthorizeTokenException
 */
fun decodedJWT(token: String, props: JwtProperties): DecodedJWT {
    val algorithm: Algorithm = Algorithm.HMAC256(props.secret)
    return try {
        JWT.require(algorithm)
           .withIssuer(props.issuer)
           .withSubject(props.subject)
           .build()
           .verify(token)
    } catch (e: Exception) {
        when (e) {
            is TokenExpiredException -> throw BadAuthorizeTokenException("토큰이 만료되엇습니다.")
            else -> throw BadAuthorizeTokenException()
        }
    }
}

/**
 * claim으로부터 userId 추출
 * @param decodedJWT
 * @return String
 */
fun memberIdFromJWT(decodedJWT: DecodedJWT): Long {
    //return decodedJWT.getClaim("memberId").asLong()
    return decodedJWT.claims["memberId"]?.asLong() ?: throw BadAuthorizeTokenException()
}

/**
 * claim으로부터 password 추출
 * @param decodedJWT
 * @return String
 */
fun emailFromJWT(decodedJWT: DecodedJWT): String {
    //return decodedJWT.getClaim("email").asString()
    return decodedJWT.claims["email"]?.asString() ?: throw BadAuthorizeTokenException()
}