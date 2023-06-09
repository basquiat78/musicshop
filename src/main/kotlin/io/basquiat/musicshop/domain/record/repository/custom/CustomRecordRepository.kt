package io.basquiat.musicshop.domain.record.repository.custom

import io.basquiat.musicshop.domain.record.model.entity.Record
import org.springframework.data.relational.core.sql.SqlIdentifier
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CustomRecordRepository {
    fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record>
    fun findAllRecords(whereClause: String = "", orderClause: String = "", limitClause: String = ""): Flux<Record>
}