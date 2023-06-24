package io.basquiat.musicshop.domain.member.repository

import io.basquiat.musicshop.common.repository.BaseRepository
import io.basquiat.musicshop.domain.member.model.entity.Member
import io.basquiat.musicshop.domain.member.repository.custom.CustomMemberRepository

interface MemberRepository: BaseRepository<Member, Long>, CustomMemberRepository {
    override suspend fun findById(id: Long): Member?
    suspend fun findByEmail(email: String): Member?
    suspend fun findByIdAndEmail(id: Long, email: String): Member?
}