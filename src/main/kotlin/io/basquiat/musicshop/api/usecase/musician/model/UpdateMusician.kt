package io.basquiat.musicshop.api.usecase.musician.model

import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.domain.musician.model.Musician
import io.basquiat.musicshop.domain.musician.model.code.Genre
import org.springframework.data.relational.core.sql.SqlIdentifier

data class UpdateMusician(
    val name: String?,
    val genre: Genre?,
) {
    fun createAssignments(musician: Musician): Pair<Musician, MutableMap<SqlIdentifier, Any>> {
        val assignments = mutableMapOf<SqlIdentifier, Any>()
        name?.let {
            assignments[SqlIdentifier.unquoted("name")] = it
            musician.name = it
        }
        genre?.let {
            assignments[SqlIdentifier.unquoted("genre")] = it
            musician.genre = it
        }
        if(assignments.isEmpty()) {
            throw BadParameterException("업데이트 정보가 누락되었습니다. [name, genre] 정보를 확인하세요.")
        }
        return musician to assignments
    }
}