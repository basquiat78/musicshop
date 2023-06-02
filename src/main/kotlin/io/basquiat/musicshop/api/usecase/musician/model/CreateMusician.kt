package io.basquiat.musicshop.api.usecase.musician.model

import io.basquiat.musicshop.domain.musician.model.code.Genre
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateMusician(
    @field:NotNull
    @field:Size(min = 2, message = "뮤지션의 이름이 누락되었습니다. 최소 한 글자 이상이어야 합니다.")
    val name: String,
    @field:NotNull(message = "장르 정보가 누락되었습니다.")
    val genre: Genre?,
)