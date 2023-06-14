package io.basquiat.musicshop.domain.musician.repository.custom

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import kotlinx.coroutines.flow.Flow
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.sql.SqlIdentifier

interface CustomMusicianRepository {
    suspend fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Musician
    fun musiciansByQuery(match: Query): Flow<Musician>
    suspend fun totalCountByQuery(match: Query): Long
    suspend fun musicianWithRecords(id: Long): Musician?
}