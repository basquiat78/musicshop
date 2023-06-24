package io.basquiat.musicshop.domain.record.service

import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.OrderSpecifier
import io.basquiat.musicshop.common.extensions.findByIdOrThrow
import io.basquiat.musicshop.domain.record.repository.RecordRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

@Service
class ReadRecordService(
    private val recordRepository: RecordRepository,
) {

    suspend fun recordById(id: Long) = recordRepository.findById(id)
    suspend fun recordByIdOrThrow(id: Long, message: String? = null) = recordRepository.findByIdOrThrow(id, message)
    suspend fun recordCountByMusician(musicianId: Long) = recordRepository.countByMusicianId(musicianId)

    fun recordByMusicianId(musicianId: Long, pageable: Pageable) = recordRepository.findByMusicianId(musicianId, pageable)
    fun allRecords(condition: BooleanBuilder, pagination: Pair<List<OrderSpecifier<*>>, PageRequest>) =
        recordRepository.findAllRecords(condition, pagination)

}