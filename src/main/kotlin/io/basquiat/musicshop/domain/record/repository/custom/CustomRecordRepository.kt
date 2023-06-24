package io.basquiat.musicshop.domain.record.repository.custom

import io.basquiat.musicshop.domain.record.model.entity.Record
import kotlinx.coroutines.flow.Flow
import org.jooq.Condition
import org.jooq.Field
import org.jooq.SortField
import org.springframework.data.domain.PageRequest

interface CustomRecordRepository {
    suspend fun updateRecord(id: Long, assignments: MutableMap<Field<*>, Any>): Int
    fun findAllRecords(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Record>
}