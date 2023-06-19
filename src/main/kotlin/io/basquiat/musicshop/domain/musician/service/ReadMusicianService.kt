package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.common.extensions.findByIdOrThrow
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.springframework.data.domain.Pageable
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Service

@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)
    fun musiciansByQuery(match: Query) = musicianRepository.musiciansByQuery(match)

    suspend fun musicianById(id: Long) = musicianRepository.findById(id)
    suspend fun musicianByIdOrThrow(id: Long, message: String? = null) = musicianRepository.findByIdOrThrow(id, message)
    suspend fun totalCount() = musicianRepository.count()
    suspend fun totalCountByQuery(match: Query) = musicianRepository.totalCountByQuery(match)
    suspend fun musicianWithRecords(id: Long) = musicianRepository.musicianWithRecords(id)
}