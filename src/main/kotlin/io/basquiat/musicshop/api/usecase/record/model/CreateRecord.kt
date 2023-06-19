package io.basquiat.musicshop.api.usecase.record.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.domain.record.model.code.ReleasedType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreateRecord(
    @field:Min(1, message = "뮤지션 아이디가 누락되었습니다.")
    var musicianId: Long,
    @field:NotNull
    @field:Size(min = 1, message = "음반명이 누락되었습니다. 최소 한 글자 이상이어야 합니다.")
    val title: String,
    @field:NotNull(message = "레이블 정보가 누락되었습니다.")
    var label: String,
    @field:NotNull(message = "음반 형태 정보가 누락되었습니다.")
    @field:EnumCheck(enumClazz = ReleasedType::class, message = "releasedType 필드는 SINGLE, FULL, EP, OST, COMPILATION, LIVE, MIXTAPE 만 가능합니다.")
    var releasedType: String,
    @field:Min(0, message = "음반 발매일이 누락되었습니다.")
    var releasedYear: Int,
    @field:NotNull(message = "음반 포맷 형식이 누락되엇습니다.")
    var format: String,
)