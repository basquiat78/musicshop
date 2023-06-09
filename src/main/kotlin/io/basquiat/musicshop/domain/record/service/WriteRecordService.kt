package io.basquiat.musicshop.domain.record.service

import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.RecordRepository
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class WriteRecordService(
    private val recordRepository: RecordRepository,
) {
    fun create(record: Record): Mono<Record> {
        return recordRepository.save(record)
    }

    fun update(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record> {
        return recordRepository.updateRecord(record, assignments)
    }
}