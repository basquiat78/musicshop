package io.basquiat.musicshop.domain.musician.repository.custom

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import kotlinx.coroutines.flow.Flow
import org.jooq.Condition
import org.jooq.Field
import org.jooq.SortField
import org.springframework.data.domain.PageRequest

interface CustomMusicianRepository {
    suspend fun updateMusician(id: Long, assignments: MutableMap<Field<*>, Any>): Int
    fun musiciansByQuery(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Musician>
    suspend fun totalCountByQuery(conditions: List<Condition>): Long
    suspend fun musicianWithRecords(id: Long): Musician?
}