package io.basquiat.musicshop.domain.musician.repository

import io.basquiat.musicshop.domain.musician.model.Musician
import io.basquiat.musicshop.domain.musician.repository.custom.CustomMusicianRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface MusicianRepository: ReactiveCrudRepository<Musician, Long>, CustomMusicianRepository {
    override fun findById(id: Long): Mono<Musician>
    fun findAllBy(pageable: Pageable): Flux<Musician>
}