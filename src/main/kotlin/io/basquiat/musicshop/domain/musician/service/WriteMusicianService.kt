package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.domain.musician.model.Musician
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.springframework.data.relational.core.sql.SqlIdentifier
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class WriteMusicianService(
    private val musicianRepository: MusicianRepository,
) {

    fun create(musician: Musician): Mono<Musician> {
        return musicianRepository.save(musician)
    }

    fun update(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician> {
        return musicianRepository.updateMusician(musician, assignments)
    }
}