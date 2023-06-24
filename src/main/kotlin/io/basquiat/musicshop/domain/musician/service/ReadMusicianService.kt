package io.basquiat.musicshop.domain.musician.service

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import io.basquiat.musicshop.common.extensions.findByIdOrThrow
import io.basquiat.musicshop.domain.musician.repository.MusicianRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ReadMusicianService(
    private val musicianRepository: MusicianRepository,
) {
    fun musicians(pageable: Pageable) = musicianRepository.findAllBy(pageable)
    fun musiciansByQuery(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>) =
        musicianRepository.musiciansByQuery(condition, pagination)

    suspend fun musicianById(id: Long) = musicianRepository.findById(id)
    suspend fun musicianByIdOrThrow(id: Long, message: String? = null) = musicianRepository.findByIdOrThrow(id, message)
    suspend fun totalCount() = musicianRepository.count()
    suspend fun totalCountByQuery(condition: BooleanBuilder) = musicianRepository.totalCountByQuery(condition)
    suspend fun musicianWithRecords(id: Long) = musicianRepository.musicianWithRecords(id)
}