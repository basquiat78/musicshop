package io.basquiat.musicshop.api.usecase.musician.model

import io.basquiat.musicshop.common.constraint.EnumCheck
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.common.utils.isParamBlankThrow
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import org.springframework.data.relational.core.sql.SqlIdentifier

data class UpdateMusician(
    val name: String? = null,
    @field:EnumCheck(enumClazz = Genre::class, permitNull = true, message = "genre 필드는 POP, ROCK, HIPHOP, JAZZ, CLASSIC, WORLDMUSIC, ETC 만 가능합니다.")
    val genre: String? = null,
) {
    fun createAssignments(musician: Musician): Pair<Musician, MutableMap<SqlIdentifier, Any>> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        name?.let {
            isParamBlankThrow(it)
            assignments[SqlIdentifier.unquoted("name")] = it
            musician.name = it
        }
        genre?.let {
            assignments[SqlIdentifier.unquoted("genre")] = it
            musician.genre = Genre.valueOf(it.uppercase())
        }
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return musician to assignments
    }
}