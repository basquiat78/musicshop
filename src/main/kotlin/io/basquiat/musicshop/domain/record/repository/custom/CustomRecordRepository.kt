package io.basquiat.musicshop.domain.record.repository.custom

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import io.basquiat.musicshop.domain.record.model.entity.Record
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.PageRequest

interface CustomRecordRepository {
    suspend fun updateRecord(id: Long, assignments: Pair<List<Path<*>>, List<Any>>): Long
    fun findAllRecords(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>): Flow<Record>
}