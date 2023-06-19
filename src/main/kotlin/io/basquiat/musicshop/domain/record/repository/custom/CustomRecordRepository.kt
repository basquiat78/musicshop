package io.basquiat.musicshop.domain.record.repository.custom

import io.basquiat.musicshop.domain.record.model.entity.Record
import kotlinx.coroutines.flow.Flow
import org.springframework.data.relational.core.sql.SqlIdentifier

interface CustomRecordRepository {
    suspend fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Record
    fun findAllRecords(whereClause: String = "", orderClause: String = "", limitClause: String = ""): Flow<Record>
}