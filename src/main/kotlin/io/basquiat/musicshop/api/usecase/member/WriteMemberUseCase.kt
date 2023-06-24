package io.basquiat.musicshop.api.usecase.member

import io.basquiat.musicshop.api.usecase.member.model.request.SignUpRequest
import io.basquiat.musicshop.api.usecase.member.model.response.SignUpResponse
import io.basquiat.musicshop.domain.member.service.MemberCacheService
import io.basquiat.musicshop.domain.member.service.WriteMemberService
import org.springframework.stereotype.Service

@Service
class WriteMemberUseCase(
    private val write: WriteMemberService,
    private val memberCacheService: MemberCacheService,
) {
    suspend fun signUp(request: SignUpRequest): SignUpResponse {
        val member = write.signUp(request.toCreateMember())
        return SignUpResponse.of(member)
    }

    suspend fun logout(token: String) {
        memberCacheService.removeCache(token)
    }
}