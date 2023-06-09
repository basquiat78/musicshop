package io.basquiat.musicshop.api.usecase.record

import io.basquiat.musicshop.common.builder.createNativeSortLimitClause
import io.basquiat.musicshop.common.builder.createNativeWhereClause
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ReadRecordUseCase(
    private val read: ReadRecordService,
    private val readMusician: ReadMusicianService,
) {

    fun recordById(id: Long): Mono<Record> {
        return read.recordByIdOrThrow(id)
    }

    fun recordByMusicianId(queryPage: QueryPage, musicianId: Long): Mono<Page<Record>> {
        val musician = readMusician.musicianByIdOrThrow(musicianId)
        return musician.flatMapMany { musician ->
            read.recordByMusicianId(musicianId, queryPage.fromPageable())
                .map {
                    it.musician = musician
                    it
                }
        }
        .collectList()
        .zipWith(read.recordCountByMusician(musicianId))
        .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }

    }

    fun allRecords(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Flux<Record> {
        val prefix = "record"
        val clazz = Record::class
        val whereClause = createNativeWhereClause(prefix, clazz, matrixVariable)
        val (orderSql, limitSql) = createNativeSortLimitClause(prefix, clazz, queryPage)
        return read.allRecords(whereClause = whereClause, orderClause = orderSql, limitClause = limitSql)
    }

}
