package io.basquiat.musicshop.domain.member.repository.custom

interface CustomMemberRepository {
    suspend fun existsByEmail(email: String): Boolean
}