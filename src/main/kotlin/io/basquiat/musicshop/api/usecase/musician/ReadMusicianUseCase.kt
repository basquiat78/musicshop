package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.common.builder.createQuery
import io.basquiat.musicshop.common.extensions.countZipWith
import io.basquiat.musicshop.common.extensions.map
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.model.entity.QMusician.musician
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap

@Service
class ReadMusicianUseCase(
    private val read: ReadMusicianService,
) {

    suspend fun musicianById(id: Long): Musician {
        return read.musicianByIdOrThrow(id)
    }

    suspend fun musiciansByQuery(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Page<Musician> {
        val condition = createQuery(matrixVariable, musician)
        return read.musiciansByQuery(createQuery(matrixVariable, musician), queryPage.pagination(musician))
                   .toList()
                   .countZipWith(read.totalCountByQuery(condition))
                   .map { ( musicians, count) -> PageImpl(musicians.toList(), queryPage.fromPageable(), count)}
    }

}
