package io.basquiat.musicshop.domain.musician.repository.custom

import io.basquiat.musicshop.domain.musician.model.entity.Musician
import org.springframework.data.relational.core.query.Query
import org.springframework.data.relational.core.sql.SqlIdentifier
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface CustomMusicianRepository {
    fun updateMusician(musician: Musician, assignments: MutableMap<SqlIdentifier, Any>): Mono<Musician>
    fun musiciansByQuery(match: Query): Flux<Musician>
    fun totalCountByQuery(match: Query): Mono<Long>

    fun musicianWithRecords(id: Long): Mono<Musician>
}