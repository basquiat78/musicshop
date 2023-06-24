package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.jooq.Field
import org.springframework.stereotype.Service

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    suspend fun create(musician: Musician): Musician {
        return musicianRepository.save(musician)
    }
    suspend fun update(musicianId: Long, assignments: MutableMap<Field<*>, Any>): Int {
        return musicianRepository.updateMusician(musicianId, assignments)
    }
}