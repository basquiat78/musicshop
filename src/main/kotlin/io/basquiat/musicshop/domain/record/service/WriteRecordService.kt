package io.basquiat.musicshop.domain.record.service

import com.querydsl.core.types.Path
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.RecordRepository
import org.springframework.stereotype.Service

@Service
class WriteRecordService(
    private val recordRepository: RecordRepository,
) {
    suspend fun create(record: Record) = recordRepository.save(record)
    suspend fun update(id: Long, assignments: Pair<List<Path<*>>, List<Any>>) = recordRepository.updateRecord(id, assignments)
}