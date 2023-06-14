package io.basquiat.musicshop.api.router.musician

import io.basquiat.musicshop.api.router.musician.model.CreateMusician
import io.basquiat.musicshop.api.router.musician.model.UpdateMusician
import io.basquiat.musicshop.common.utils.validate
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.musician.service.WriteMusicianService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.net.URI

@Service
class WriteMusicianHandler(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    fun insert(request: ServerRequest): Mono<ServerResponse> {
        return request.bodyToMono(CreateMusician::class.java)
                      .flatMap {
                            validate(it)
                            write.create(it.toEntity())
                      }
                      .flatMap {
                            ServerResponse.created(URI.create("/api/v1/musicians/${it.id}"))
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                      }
    }

    fun update(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return request.bodyToMono(UpdateMusician::class.java)
                      .flatMap {
                          validate(it)
                          read.musicianByIdOrThrow(id)
                              .flatMap { entity ->
                                  val (musician, assignments) = it.createAssignments(entity)
                                  write.update(musician, assignments)
                              }.then(read.musicianById(id))
                      }
                      .flatMap {
                            ServerResponse.ok()
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .body(it.toMono(), Musician::class.java)
                      }
    }

}