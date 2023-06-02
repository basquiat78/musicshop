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

    fun musicianById(id: Long) = musicianRepository.findById(id)

    fun musicianByIdOrThrow(id: Long) = musicianRepository.findByIdOrThrow(id)

    fun totalCount() = musicianRepository.count()

    fun musiciansByQuery(match: Query) = musicianRepository.musiciansByQuery(match)

    fun totalCountByQuery(match: Query) = musicianRepository.totalCountByQuery(match)

}