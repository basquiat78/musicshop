package io.basquiat.musicshop.api.usecase.musician.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.domain.musician.model.code.Genre
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class CreateMusician(
    @field:NotNull(message = "뮤지션의 이름이 누락되었습니다. 최소 한 글자 이상이어야 합니다.")
    @field:NotBlank(message = "뮤지션의 이름에 빈 공백은 허용하지 않습니다.")
    val name: String,
    @field:NotNull(message = "장르 정보가 누락되었습니다.")
    @field:EnumCheck(enumClazz = Genre::class, message = "genre 필드는 POP, ROCK, HIPHOP, JAZZ, CLASSIC, WORLDMUSIC, ETC 만 가능합니다.")
    val genre: String,
)