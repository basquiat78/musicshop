package io.basquiat.musicshop.domain.member.model.dto

data class JwtClaim(
    val memberId: Long,
    val email: String,
)