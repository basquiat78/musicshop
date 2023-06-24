package io.basquiat.musicshop.domain.member.model.dto

data class SignInResponse(
    val memberId: Long,
    val email: String,
    val token: String,
)
