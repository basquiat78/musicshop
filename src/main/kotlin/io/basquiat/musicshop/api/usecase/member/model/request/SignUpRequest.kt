package io.basquiat.musicshop.api.usecase.member.model.request

import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encrypt
import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.encryptPassword
import io.basquiat.musicshop.domain.member.model.entity.Member
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

class SignUpRequest(
    @field:NotBlank(message = "이메일은 필수입니다.")
    @field:Email(message = "이메일 형식이 아닙니다. 형식에 맞는 이메일을 입력해주세요.")
    val email: String,
    @field:NotBlank(message = "사용자 이름은 필수입니다.")
    val name: String,
    @field:NotBlank(message = "비빌번호는 필수입니다.")
    val password: String,
) {
    fun toCreateMember(): Member {
        return Member(
            name = encrypt(name),
            email = encrypt(email),
            password = encryptPassword(password)
        )
    }
}