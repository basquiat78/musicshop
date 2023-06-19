package io.basquiat.musicshop.domain.musician.repository

import io.basquiat.musicshop.common.repository.BaseRepository
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.repository.custom.CustomMusicianRepository
import kotlinx.coroutines.flow.Flow
import org.springframework.data.domain.Pageable

interface MusicianRepository: BaseRepository<Musician, Long>, CustomMusicianRepository {
    override suspend fun findById(id: Long): Musician?
    fun findAllBy(pageable: Pageable): Flow<Musician>
}