package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.common.exception.BadParameterException
import io.basquiat.musicshop.domain.musician.model.Musician
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.musician.service.WriteMusicianService
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class WriteMusicianUseCase(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    fun insert(command: CreateMusician): Mono<Musician> {
        val created = Musician(name = command.name, genre = command.genre)
        return write.create(created)
    }

    fun update(id: Long, command: UpdateMusician): Mono<Musician> {
        return read.musicianByIdOrThrow(id).flatMap { musician ->
            val (musician, assignments) = command.createAssignments(musician)
            write.update(musician, assignments)
        }.onErrorResume {
            Mono.error(BadParameterException(it.message))
        }
        .then(read.musicianById(id))
    }

}