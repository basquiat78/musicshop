package io.basquiat.musicshop.api.usecase.member

import io.basquiat.musicshop.api.usecase.member.model.request.SignInRequest
import io.basquiat.musicshop.domain.member.model.dto.SignInResponse
import io.basquiat.musicshop.domain.member.model.entity.Member
import io.basquiat.musicshop.domain.member.service.MemberCacheService
import io.basquiat.musicshop.domain.member.service.ReadMemberService
import org.springframework.stereotype.Service

@Service
class ReadMemberUseCase(
    private val read: ReadMemberService,
    private val memberCacheService: MemberCacheService,
) {

    suspend fun signIn(request: SignInRequest): SignInResponse {
        return read.signIn(request.email, request.password)
    }

    suspend fun memberByToken(token: String): Member {
        return memberCacheService.memberByJWT(token)
    }

}