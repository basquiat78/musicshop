package io.basquiat.musicshop.domain.record.service

import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.RecordRepository
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Service

@Service
class WriteRecordService(
    private val recordRepository: RecordRepository,
) {
    suspend fun create(record: Record) = recordRepository.save(record)
    suspend fun update(record: Record, assignments: MutableMap<SqlIdentifier, Any>) = recordRepository.updateRecord(record, assignments)
}