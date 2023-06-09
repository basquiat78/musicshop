package io.basquiat.musicshop.api.usecase.musician

import io.basquiat.musicshop.common.builder.createQuery
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.stereotype.Service
import org.springframework.util.MultiValueMap
import reactor.core.publisher.Mono

@Service
class ReadMusicianUseCase(
    private val read: ReadMusicianService,
) {

    fun musicianById(id: Long): Mono<Musician> {
        return read.musicianByIdOrThrow(id)
    }

    fun musiciansByQuery(queryPage: QueryPage, matrixVariable: MultiValueMap<String, Any>): Mono<Page<Musician>> {
        val match = createQuery(matrixVariable)
        return read.musiciansByQuery(queryPage.pagination(match))
                   .collectList()
                   .zipWith(read.totalCountByQuery(match))
                   .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }
    }

}
