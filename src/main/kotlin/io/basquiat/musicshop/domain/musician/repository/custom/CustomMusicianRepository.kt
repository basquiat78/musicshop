package io.basquiat.musicshop.domain.musician.repository.custom

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.PageRequest

interface CustomMusicianRepository {
    suspend fun updateMusician(id: Long, assignments: Pair<List<Path<*>>, List<Any>>): Long
    fun musiciansByQuery(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>): Flow<Musician>
    suspend fun totalCountByQuery(condition: BooleanBuilder): Long
    suspend fun musicianWithRecords(id: Long): Musician
}