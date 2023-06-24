package io.basquiat.musicshop.domain.musician.repository.custom.impl

import com.infobip.spring.data.r2dbc.QuerydslR2dbcRepository
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import com.querydsl.core.types.Path
import com.querydsl.core.types.Projections
import io.basquiat.musicshop.common.utils.notFound
import io.basquiat.musicshop.domain.musician.model.MusicianDto
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.model.entity.QMusician.musician
import io.basquiat.musicshop.domain.musician.repository.custom.CustomMusicianRepository
import io.basquiat.musicshop.domain.record.model.entity.QRecord.record
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.data.domain.PageRequest
import org.springframework.r2dbc.core.awaitSingle
import org.springframework.r2dbc.core.flow


interface MusicianQueryDslRepository: QuerydslR2dbcRepository<Musician, Long>

class CustomMusicianRepositoryImpl(
    private val queryDsl: MusicianQueryDslRepository,
): CustomMusicianRepository {

    override suspend fun updateMusician(id: Long, assignments: Pair<List<Path<*>>, List<Any>>): Long {
        return queryDsl.update {
            it.set(assignments.first, assignments.second)
              .where(musician.id.eq(id))
        }.awaitSingle()
    }

    override fun musiciansByQuery(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>): Flow<Musician> {
        return queryDsl.query {
            it.select(musician)
              .from(musician)
              .where(condition)
              .orderBy(*pagination.first.toTypedArray())
              .limit(pagination.second.pageSize.toLong())
              .offset(pagination.second.offset)
        }.flow()
    }

    override suspend fun totalCountByQuery(condition: BooleanBuilder): Long {
        return queryDsl.query {
            it.select(musician.id.count())
              .from(musician)
              .where(condition)
        }.awaitSingle()
    }

    override suspend fun musicianWithRecords(id: Long): Musician {
        val dtoList =  queryDsl.query {
            it.select(
                Projections.constructor(MusicianDto::class.java,
                    musician.id,
                    musician.name,
                    musician.genre,
                    musician.createdAt,
                    musician.updatedAt,
                    record.id.`as`("record_id"),
                    record.title,
                    record.label,
                    record.releasedType,
                    record.releasedYear,
                    record.format,
                    record.createdAt.`as`("record_created_at"),
                    record.updatedAt.`as`("record_updated_at")
                )
            )
            .from(musician)
            .innerJoin(record).on(musician.id.eq(record.musicianId))
            .where(musician.id.eq(id))
        }.flow().toList()
        if(dtoList.isEmpty()) notFound("뮤지션 아이디 [$id]로 조회된 정보가 없습니다.")
        val musician = dtoList[0].toMusician()
        val records = dtoList.map { it.toRecord() }
        musician.records = records
        return musician
    }

}
