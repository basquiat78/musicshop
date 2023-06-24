package io.basquiat.musicshop.domain.record.repository.custom.impl

import com.infobip.spring.data.r2dbc.QuerydslR2dbcRepository
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import com.querydsl.core.types.Projections
import io.basquiat.musicshop.domain.musician.model.entity.QMusician.musician
import io.basquiat.musicshop.domain.record.model.RecordDto
import io.basquiat.musicshop.domain.record.model.entity.QRecord.record
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.custom.CustomRecordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.data.domain.PageRequest
import org.springframework.r2dbc.core.flow

interface RecordQueryDslRepository: QuerydslR2dbcRepository<Record, Long>

class CustomRecordRepositoryImpl(
    private val queryDsl: RecordQueryDslRepository,
): CustomRecordRepository {

    override suspend fun updateRecord(id: Long, assignments: Pair<List<Path<*>>, List<Any>>): Long {
        return queryDsl.update {
            it.set(assignments.first, assignments.second)
              .where(record.id.eq(id))
        }.awaitSingle()
    }

    override fun findAllRecords(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>): Flow<Record> {
        return queryDsl.query {
            it.select(
                Projections.constructor(RecordDto::class.java,
                    record.id,
                    record.title,
                    record.label,
                    record.releasedType,
                    record.releasedYear,
                    record.format,
                    record.createdAt,
                    record.updatedAt,
                    record.musicianId,
                    musician.name,
                    musician.genre,
                    musician.createdAt.`as`("musician_created_at"),
                    musician.updatedAt.`as`("musician_updated_at")
                )
            )
            .from(record)
            .innerJoin(musician).on(musician.id.eq(record.musicianId))
            .where(condition)
            .orderBy(*pagination.first.toTypedArray())
            .limit(pagination.second.pageSize.toLong())
            .offset(pagination.second.offset)
        }.flow()
        .map {
            val record = it.toRecord()
            val musician = it.toMusician()
            record.musician = musician
            record
        }
    }

}
