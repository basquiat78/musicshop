package io.basquiat.musicshop.domain.record.repository

import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.custom.CustomRecordRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.r2dbc.repository.R2dbcRepository
import org.springframework.data.repository.query.Param
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface RecordRepository: R2dbcRepository<Record, Long>, CustomRecordRepository {
    override fun findById(id: Long): Mono<Record>
    fun findByMusicianId(id: Long, pageable: Pageable): Flux<Record>
    @Query("SELECT COUNT(id) FROM record WHERE musician_id = :musicianId")
    fun countByMusicianId(@Param("musicianId") musicianId: Long): Mono<Long>

    @Query("""
            SELECT musician.name AS musicianName,
                   musician.genre,
                   musician.created_at AS mCreatedAt,
                   musician.updated_at AS mUpdatedAt,
                   record.*
              FROM record
              INNER JOIN musician
              ON record.musician_id = musician.id
        """)
    fun findRecords(): Flux<Record>

}