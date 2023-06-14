package io.basquiat.musicshop.api.router.record

import io.basquiat.musicshop.common.builder.createNativeSortLimitClause
import io.basquiat.musicshop.common.builder.createNativeWhereClause
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.common.utils.searchMatrixVariable
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Service
class ReadRecordHandler(
    private val read: ReadRecordService,
    private val readMusician: ReadMusicianService,
) {

    fun recordById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(read.recordByIdOrThrow(id, "id [$id]로 조회되는 레코드가 없습니다."), Record::class.java)
    }

    fun recordByMusicianId(request: ServerRequest): Mono<ServerResponse> {
        val musicianId = request.pathVariable("musicianId").toLong()
        val queryPage = QueryPage.fromServerResponse(request)
        val musician = readMusician.musicianByIdOrThrow(musicianId)
        val page = musician.flatMapMany { entity ->
                                                        read.recordByMusicianId(musicianId, queryPage.fromPageable())
                                                            .map {
                                                                it.musician = entity
                                                                it
                                                            }
                                                  }
                                                  .collectList()
                                                  .zipWith(read.recordCountByMusician(musicianId))
                                                  .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(page, Page::class.java)
    }

    fun allRecords(request: ServerRequest): Mono<ServerResponse> {
        val queryPage = QueryPage.fromServerResponse(request)
        val matrixVariables = searchMatrixVariable(request)
        val prefix = "record"
        val clazz = Record::class
        val whereClause = createNativeWhereClause(prefix, clazz, matrixVariables)
        val (orderSql, limitSql) = createNativeSortLimitClause(prefix, clazz, queryPage)
        val flux =  read.allRecords(whereClause = whereClause, orderClause = orderSql, limitClause = limitSql)
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(flux, Record::class.java)
    }

}
