package io.basquiat.musicshop.domain.record.repository.custom.impl

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.custom.CustomRecordRepository
import io.basquiat.musicshop.entity.tables.JMusician
import io.basquiat.musicshop.entity.tables.JRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitSingle
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SortField
import org.springframework.data.domain.PageRequest

class CustomRecordRepositoryImpl(
    private val query: DSLContext,
): CustomRecordRepository {

    override suspend fun updateRecord(id: Long, assignments: MutableMap<Field<*>, Any>): Int {
        val record = JRecord.RECORD
        return query.update(record)
                    .set(assignments)
                    .where(record.ID.eq(id))
                    .awaitSingle()
    }

    override fun findAllRecords(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Record> {
        val record = JRecord.RECORD
        val musician = JMusician.MUSICIAN
        val sqlBuilder= query.select(
            record,
            musician
        )
        .from(record)
        .join(musician).on(record.MUSICIAN_ID.eq(musician.ID))
        .where()

        if(conditions.isNotEmpty()) {
            conditions.forEach { sqlBuilder.and(it) }
        }

        if(pagination.first.isNotEmpty()) {
            sqlBuilder.orderBy(pagination.first)
        }
        sqlBuilder.limit(pagination.second.pageSize)
                  .offset(pagination.second.offset)

        return sqlBuilder.asFlow()
                         .map {
                               val selectRecord = it.value1().into(Record::class.java)
                               val selectMusician = it.value2().into(Musician::class.java)
                               selectRecord.musician = selectMusician
                               selectRecord
                         }

    }


}
