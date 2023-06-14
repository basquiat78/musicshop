package io.basquiat.musicshop.api.router.musician

import io.basquiat.musicshop.common.builder.createQuery
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.common.utils.searchMatrixVariable
import io.basquiat.musicshop.domain.musician.model.entity.Musician
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import reactor.core.publisher.Mono

@Service
class ReadMusicianHandler(
    private val read: ReadMusicianService,
) {

    fun musicianById(request: ServerRequest): Mono<ServerResponse> {
        val id = request.pathVariable("id").toLong()
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(read.musicianByIdOrThrow(id, "id [$id]로 조회되는 뮤지션이 없습니다."), Musician::class.java)
    }

    fun musiciansByQuery(request: ServerRequest): Mono<ServerResponse> {
        val queryPage = QueryPage.fromServerResponse(request)
        val matrixVariables = searchMatrixVariable(request)
        val match = createQuery(matrixVariables)
        val page = read.musiciansByQuery(queryPage.pagination(match))
                                               .collectList()
                                               .zipWith(read.totalCountByQuery(queryPage.pagination(match)))
                                               .map { tuple -> PageImpl(tuple.t1, queryPage.fromPageable(), tuple.t2) }
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .body(page, Page::class.java)
    }

}
