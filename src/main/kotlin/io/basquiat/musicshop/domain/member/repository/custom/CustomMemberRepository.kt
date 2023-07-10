package io.basquiat.musicshop.domain.member.repository.custom

import io.basquiat.musicshop.domain.member.model.entity.Member

interface CustomMemberRepository {
    suspend fun existsByEmail(email: String): Boolean
    suspend fun memberWithRoles(id: Long, email: String): Member?
}