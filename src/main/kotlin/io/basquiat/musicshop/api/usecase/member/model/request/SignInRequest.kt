package io.basquiat.musicshop.api.usecase.member.model.request

import jakarta.validation.constraints.NotBlank

data class SignInRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    val email: String,
    @field:NotBlank(message = "비빌번호는 필수입니다.")
    val password: String,
)