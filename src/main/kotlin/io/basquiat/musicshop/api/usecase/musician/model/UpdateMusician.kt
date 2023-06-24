package io.basquiat.musicshop.api.usecase.musician.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.utils.isParamBlankThrow
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.entity.tables.JMusician
import org.jooq.Field

data class UpdateMusician(
    val name: String? = null,
    @field:EnumCheck(enumClazz = Genre::class, permitNull = true, message = "genre 필드는 POP, ROCK, HIPHOP, JAZZ, CLASSIC, WORLDMUSIC, ETC 만 가능합니다.")
    val genre: String? = null,
) {
    fun createAssignments(): MutableMap<Field<*>, Any> {
        val assignments = mutableMapOf<Field<*>, Any>()
        name?.let {
            isParamBlankThrow(it)
            assignments[JMusician.MUSICIAN.NAME] = it
        }
        genre?.let {
            assignments[JMusician.MUSICIAN.GENRE] = it
        }
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return assignments
    }
}