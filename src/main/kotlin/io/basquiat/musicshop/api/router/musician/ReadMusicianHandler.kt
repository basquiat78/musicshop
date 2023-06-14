package io.basquiat.musicshop.api.router.musician

import io.basquiat.musicshop.common.builder.createQuery
import io.basquiat.musicshop.common.extensions.countZipWith
import io.basquiat.musicshop.common.extensions.map
import io.basquiat.musicshop.common.model.request.QueryPage
import io.basquiat.musicshop.common.utils.searchMatrixVariable
import io.basquiat.musicshop.domain.musician.service.ReadMusicianService
import kotlinx.coroutines.flow.toList
import org.springframework.data.domain.PageImpl
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.ServerRequest
import org.springframework.web.reactive.function.server.ServerResponse
import org.springframework.web.reactive.function.server.bodyValueAndAwait

@Service
class ReadMusicianHandler(
    private val read: ReadMusicianService,
) {

    suspend fun musicianById(request: ServerRequest): ServerResponse {
        val id = request.pathVariable("id").toLong()
        val musician = read.musicianByIdOrThrow(id, "id [$id]로 조회되는 뮤지션이 없습니다.")
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValueAndAwait(musician)
    }

    suspend fun musiciansByQuery(request: ServerRequest): ServerResponse {
        val queryPage = QueryPage.fromServerResponse(request)
        val matrixVariables = searchMatrixVariable(request)
        val match = createQuery(matrixVariables)
        val page = read.musiciansByQuery(queryPage.pagination(match))
                                         .toList()
                                         .countZipWith(read.totalCountByQuery(queryPage.pagination(match)))
                                         .map { (musicians, count) -> PageImpl(musicians.toList(), queryPage.fromPageable(), count) }
        return ServerResponse.ok()
                             .contentType(MediaType.APPLICATION_JSON)
                             .bodyValueAndAwait(page)
    }

}
