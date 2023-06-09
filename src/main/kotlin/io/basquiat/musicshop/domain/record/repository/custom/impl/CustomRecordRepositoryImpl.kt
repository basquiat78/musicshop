package io.basquiat.musicshop.domain.record.repository.custom.impl

import io.basquiat.musicshop.domain.record.mapper.RecordMapper
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.custom.CustomRecordRepository
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.relational.core.query.Criteria.where
import org.springframework.data.relational.core.query.Query.query
import org.springframework.data.relational.core.query.Update
import org.springframework.data.relational.core.sql.SqlIdentifier
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class CustomRecordRepositoryImpl(
    private val query: R2dbcEntityTemplate,
    private val recordMapper: RecordMapper,
): CustomRecordRepository {

    override fun updateRecord(record: Record, assignments: MutableMap<SqlIdentifier, Any>): Mono<Record> {
        return query.update(Record::class.java)
                    .matching(query(where("id").`is`(record.id!!)))
                    .apply(Update.from(assignments))
                    .thenReturn(record)
    }

    override fun findAllRecords(whereClause: String, orderClause: String, limitClause: String): Flux<Record> {
        var sql = """
            SELECT musician.name AS musicianName,
                   musician.genre,
                   musician.created_at AS mCreatedAt,
                   musician.updated_at AS mUpdatedAt,
                   record.*
              FROM record
              INNER JOIN musician
              ON record.musician_id = musician.id
              WHERE 1 = 1 
              $whereClause
              $orderClause
              $limitClause
        """.trimIndent()
        return query.databaseClient
                    .sql(sql)
                    .map(recordMapper::apply)
                    .all()
    }


}
