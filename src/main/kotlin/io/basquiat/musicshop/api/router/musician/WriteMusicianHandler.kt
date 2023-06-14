package io.basquiat.musicshop.api.router.musician

import io.basquiat.musicshop.api.router.musician.model.CreateMusician
import io.basquiat.musicshop.api.router.musician.model.UpdateMusician
import io.basquiat.musicshop.common.utils.validate
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.musician.service.WriteMusicianService
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.awaitBody
import org.springframework.web.reactive.function.server.bodyValueAndAwait
import java.net.URI

@Service
class WriteMusicianHandler(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    suspend fun insert(request: ServerRequest): ServerResponse {
        val requestBody = request.awaitBody<CreateMusician>()
        return requestBody.let {
            validate(it)
            val created = write.create(it.toEntity())
            ServerResponse.created(URI.create("/api/v1/musicians/${created.id}"))
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(it)
        }
    }

    suspend fun update(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val requestBody = request.awaitBody<UpdateMusician>()
        return requestBody.let {
            validate(it)
            val target = read.musicianByIdOrThrow(id)
            val (musician, assignments) = it.createAssignments(target)
            write.update(musician, assignments)
            ServerResponse.ok()
                          .contentType(MediaType.APPLICATION_JSON)
                          .bodyValueAndAwait(read.musicianById(target.id!!)!!)
        }
    }

}