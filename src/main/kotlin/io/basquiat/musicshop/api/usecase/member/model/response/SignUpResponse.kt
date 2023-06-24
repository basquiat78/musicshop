package io.basquiat.musicshop.api.usecase.member.model.response

import io.basquiat.musicshop.common.utils.CryptoUtils.Companion.decrypt
import io.basquiat.musicshop.domain.member.model.entity.Member

data class SignUpResponse(
    val id: Long,
    val email: String,
    val name: String,
) {
    companion object {
        fun of(entity: Member): SignUpResponse {
            return SignUpResponse(
                id = entity.id!!,
                email = decrypt(entity.email),
                name = decrypt(entity.name),
            )
        }
    }
}
