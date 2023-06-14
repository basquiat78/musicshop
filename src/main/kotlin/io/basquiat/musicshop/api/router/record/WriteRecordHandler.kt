package io.basquiat.musicshop.api.router.record

import io.basquiat.musicshop.api.router.record.model.CreateRecord
import io.basquiat.musicshop.api.router.record.model.UpdateRecord
import io.basquiat.musicshop.common.utils.validate
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.service.ReadRecordService
import io.basquiat.musicshop.domain.record.service.WriteRecordService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

@Service
class WriteRecordHandler(
    private val read: ReadRecordService,
    private val write: WriteRecordService,
) {

    fun insert(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(CreateRecord::class.java)
            .flatMap {
                validate(it)
                write.create(it.toEntity())
            }
            .flatMap {
                ServerResponse.created(URI.create("/api/v1/records/${it.id}"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(it.toMono(), Record::class.java)
            }
    }

    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return request.bodyToMono(UpdateRecord::class.java)
                      .flatMap {
                            validate(it)
                            read.recordByIdOrThrow(id)
                                .flatMap { entity ->
                                    val (record, assignments) = it.createAssignments(entity)
                                    write.update(record, assignments)
                                }.then(read.recordById(id))
                      }
                      .flatMap {
                            ServerResponse.ok()
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                      }
    }

}