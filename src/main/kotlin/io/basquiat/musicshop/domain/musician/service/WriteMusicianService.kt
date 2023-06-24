package io.basquiat.musicshop.domain.musician.service

import com.querydsl.core.types.Path
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.springframework.stereotype.Service

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    suspend fun create(musician: Musician): Musician {
        return musicianRepository.save(musician)
    }
    suspend fun update(id: Long, assignments: Pair<List<Path<*>>, List<Any>>): Long {
        return musicianRepository.updateMusician(id, assignments)
    }
}