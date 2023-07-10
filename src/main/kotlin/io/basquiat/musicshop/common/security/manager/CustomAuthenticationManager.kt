package io.basquiat.musicshop.common.security.manager

import io.basquiat.musicshop.domain.member.service.MemberCacheService
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.mono
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@Component
class CustomAuthenticationManager(
    private val memberCacheService: MemberCacheService,
): ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication): Mono<Authentication> = mono {
        val authToken = authentication.credentials.toString()
        val member = memberCacheService.memberByJWT(authToken)
        val authorities = member.roles?.let { roles ->
            roles.map { SimpleGrantedAuthority(it.name) }
        }
        toAuthentication(authentication, authorities).toMono().awaitSingle()
    }

    private fun toAuthentication(
        authentication: Authentication,
        authorities: List<out GrantedAuthority>? = null
    ): UsernamePasswordAuthenticationToken {
        return UsernamePasswordAuthenticationToken(
            authentication.principal,
            authentication.credentials,
            authorities
        )
    }

}