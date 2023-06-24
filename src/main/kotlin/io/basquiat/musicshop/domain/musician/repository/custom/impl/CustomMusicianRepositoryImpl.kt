package io.basquiat.musicshop.domain.musician.repository.custom.impl

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.repository.custom.CustomMusicianRepository
import io.basquiat.musicshop.domain.record.model.entity.Record
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
import org.jooq.impl.DSL.asterisk
import org.springframework.data.domain.PageRequest
import reactor.core.publisher.Flux

class CustomMusicianRepositoryImpl(
    private val query: DSLContext,
): CustomMusicianRepository {

    override suspend fun updateMusician(id: Long, assignments: MutableMap<Field<*>, Any>): Int {
        val musician = JMusician.MUSICIAN
        return query.update(musician)
                    .set(assignments)
                    .where(musician.ID.eq(id)).awaitSingle()
    }

    override fun musiciansByQuery(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>): Flow<Musician> {
        val musician = JMusician.MUSICIAN
        val query = query.select(asterisk())
                                                  .from(musician)
                                                  .where()
        if(conditions.isNotEmpty()) {
            conditions.forEach { query.and(it) }
        }

        if(pagination.first.isNotEmpty()) {
            query.orderBy(pagination.first)
        }
        query.limit(pagination.second.pageSize)
             .offset(pagination.second.offset)
        return query.asFlow()
                    .map { it.into(Musician::class.java) }
    }

    override suspend fun totalCountByQuery(conditions: List<Condition>): Long {
        val musician = JMusician.MUSICIAN
        val sqlBuilder = query.selectCount()
                                                            .from(musician)
                                                            .where()
        if(conditions.isNotEmpty()) {
            conditions.forEach {
                sqlBuilder.and(it)
            }
        }
        return sqlBuilder.awaitSingle().value1().toLong()
    }

    override suspend fun musicianWithRecords(id: Long): Musician? {
        val musician = JMusician.MUSICIAN
        val record = JRecord.RECORD

        val sqlBuilder =
            query.select(
                musician,
                record
            )
            .from(musician)
            .leftJoin(record).on(musician.ID.eq(record.MUSICIAN_ID))
            .where(musician.ID.eq(id))
        return Flux.from(sqlBuilder)
                   .bufferUntilChanged { it.component1() }
                   .map {
                        rows ->
                            val selectMusician = rows[0].component1().into(Musician::class.java)
                            val records = rows.map { it.component2().into(Record::class.java) }
                        selectMusician.records = records
                        selectMusician
                   }.awaitSingle()
    }

}
