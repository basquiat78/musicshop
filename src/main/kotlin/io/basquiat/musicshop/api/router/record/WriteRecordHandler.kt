package io.basquiat.musicshop.api.router.record

import io.basquiat.musicshop.api.router.record.model.CreateRecord
import io.basquiat.musicshop.api.router.record.model.UpdateRecord
import io.basquiat.musicshop.common.utils.validate
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import io.basquiat.musicshop.domain.record.service.WriteRecordService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.net.URI

@Service
class WriteRecordHandler(
    private val read: ReadRecordService,
    private val write: WriteRecordService,
) {

    suspend fun insert(request: ServerRequest): ServerResponse {
        val requestBody = request.awaitBody<CreateRecord>()
        return requestBody.let {
            validate(it)
            val created = write.create(it.toEntity())
            ServerResponse.created(URI.create("/api/v1/records/${created.id}"))
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(created)
        }
    }

    suspend fun update(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val requestBody = request.awaitBody<UpdateRecord>()
        return requestBody.let {
            validate(it)
            val target = read.recordByIdOrThrow(id)
            val (record, assignments) = it.createAssignments(target)
            write.update(record, assignments)
            ServerResponse.ok()
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(read.recordById(target.id!!)!!)
        }
    }

}