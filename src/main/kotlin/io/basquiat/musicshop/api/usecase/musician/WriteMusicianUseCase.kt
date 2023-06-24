package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.api.usecase.musician.model.CreateMusician
import io.basquiat.musicshop.api.usecase.musician.model.UpdateMusician
import io.basquiat.musicshop.domain.musician.model.code.Genre
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import io.basquiat.musicshop.domain.musician.service.WriteMusicianService
import org.springframework.stereotype.Service

@Service
class WriteMusicianUseCase(
    private val read: ReadMusicianService,
    private val write: WriteMusicianService,
) {

    suspend fun insert(command: CreateMusician): Musician {
        val created = Musician(name = command.name, genre = Genre.valueOf(command.genre))
        return write.create(created)
    }

    suspend fun update(id: Long, command: UpdateMusician): Musician {
        val musician = read.musicianByIdOrThrow(id)
        write.update(musician.id!!, command.createAssignments())
        return read.musicianById(id)!!
    }

}