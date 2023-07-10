package io.basquiat.musicshop.api.usecase.member

import io.basquiat.musicshop.api.usecase.member.model.request.SignUpRequest
import io.basquiat.musicshop.api.usecase.member.model.response.SignUpResponse
import io.basquiat.musicshop.domain.member.model.code.RoleCode
import io.basquiat.musicshop.domain.member.model.entity.Role
import io.basquiat.musicshop.domain.member.service.MemberCacheService
import io.basquiat.musicshop.domain.member.service.WriteMemberService
import io.basquiat.musicshop.domain.member.service.WriteRoleService
import org.springframework.stereotype.Service

@Service
class WriteMemberUseCase(
    private val write: WriteMemberService,
    private val role: WriteRoleService,
    private val memberCacheService: MemberCacheService,
) {
    suspend fun signUp(request: SignUpRequest): SignUpResponse {
        val member = write.signUp(request.toCreateMember())
        val createRole = Role(memberId = member.id!!, roleName = RoleCode.USER.name)
        role.saveRole(createRole)
        return SignUpResponse.of(member)
    }

    suspend fun logout(token: String) {
        memberCacheService.removeCache(token)
    }
}