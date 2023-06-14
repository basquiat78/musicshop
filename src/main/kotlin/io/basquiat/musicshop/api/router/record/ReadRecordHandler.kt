package io.basquiat.musicshop.api.router.record

import io.basquiat.musicshop.common.builder.createNativeSortLimitClause
import io.basquiat.musicshop.common.builder.createNativeWhereClause
import io.basquiat.musicshop.common.extensions.countZipWith
import io.basquiat.musicshop.common.extensions.map
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.common.utils.searchMatrixVariable
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Service
class ReadRecordHandler(
    private val read: ReadRecordService,
    private val readMusician: ReadMusicianService,
) {

    suspend fun recordById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val record = read.recordByIdOrThrow(id, "id [$id]로 조회되는 레코드가 없습니다.");
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValueAndAwait(record)
    }

    suspend fun recordByMusicianId(request: ServerRequest): ServerResponse {
        val musicianId = request.pathVariable("musicianId").toLong()
        val queryPage = QueryPage.fromServerResponse(request)
        val musician = readMusician.musicianByIdOrThrow(musicianId)
        return musician.let { musician ->
           val records = read.recordByMusicianId(musician.id!!, queryPage.fromPageable())
                                         .toList()
                                         .map {
                                            it.musician = musician
                                            it
                                         }
                                         .countZipWith(read.recordCountByMusician(musicianId))
                                         .map { (records, count) -> PageImpl(records.toList(), queryPage.fromPageable(), count) }
            ServerResponse.ok()
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(records)
        }
    }

    suspend fun allRecords(request: ServerRequest): ServerResponse {
        val queryPage = QueryPage.fromServerResponse(request)
        val matrixVariables = searchMatrixVariable(request)
        val prefix = "record"
        val clazz = Record::class
        val whereClause = createNativeWhereClause(prefix, clazz, matrixVariables)
        val (orderSql, limitSql) = createNativeSortLimitClause(prefix, clazz, queryPage)
        val flow =  read.allRecords(whereClause = whereClause, orderClause = orderSql, limitClause = limitSql)
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValueAndAwait(flow.toList())
    }

}
