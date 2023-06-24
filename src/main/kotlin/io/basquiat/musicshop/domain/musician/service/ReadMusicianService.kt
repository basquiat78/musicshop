package io.basquiat.musicshop.domain.musician.service

import io.basquiat.musicshop.common.extensions.findByIdOrThrow
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.jooq.Condition
import org.jooq.SortField
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)
    fun musiciansByQuery(conditions: List<Condition>, pagination: Pair<List<SortField<*>>, PageRequest>) =
        musicianRepository.musiciansByQuery(conditions, pagination)

    suspend fun musicianById(id: Long) = musicianRepository.findById(id)
    suspend fun musicianByIdOrThrow(id: Long, message: String? = null) = musicianRepository.findByIdOrThrow(id, message)
    suspend fun totalCount() = musicianRepository.count()
    suspend fun totalCountByQuery(conditions: List<Condition>) = musicianRepository.totalCountByQuery(conditions)
    suspend fun musicianWithRecords(id: Long) = musicianRepository.musicianWithRecords(id)
}