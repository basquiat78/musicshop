package io.basquiat.musicshop.domain.record.repository

import io.basquiat.musicshop.common.repository.BaseRepository
import io.basquiat.musicshop.domain.record.model.entity.Record
import io.basquiat.musicshop.domain.record.repository.custom.CustomRecordRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.query.Param

interface RecordRepository: BaseRepository<Record, Long>, CustomRecordRepository {
    override suspend fun findById(id: Long): Record?

    fun findByMusicianId(id: Long, pageable: Pageable): Flow<Record>

    @Query("SELECT COUNT(id) FROM record WHERE musician_id = :musicianId")
    suspend fun countByMusicianId(@Param("musicianId") musicianId: Long): Long

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
    fun findRecords(): Flow<Record>

}