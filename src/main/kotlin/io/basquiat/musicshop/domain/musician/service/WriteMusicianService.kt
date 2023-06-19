package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Service

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    suspend fun create(musician: Musician): Musician {
        return musicianRepository.save(musician)
    }
    suspend fun update(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Musician {
        return musicianRepository.updateMusician(musician, assignments)
    }
}