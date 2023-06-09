package io.basquiat.musicshop.domain.record.model.code

enum class ReleasedType(
    val description: String,
) {
    SINGLE("싱글"),
    FULL("정규 음반"),
    EP("EP"),
    OST("o.s.t."),
    COMPILATION("컴필레이션 음반"),
    LIVE("라이브 음반"),
    MIXTAPE("믹스테잎");
}