package io.basquiat.musicshop.domain.record.model.code

enum class RecordFormat(
    val description: String,
) {

    TAPE("테이프"),
    CD("시디"),
    LP("엘피"),
    DIGITAL("디지털 음원"),
    DIGITALONLY("디지털 음원 온리");

}