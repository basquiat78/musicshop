package io.basquiat.musicshop.common.extensions

import io.basquiat.musicshop.common.utils.notFound
import org.springframework.data.r2dbc.repository.R2dbcRepository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

fun <T, ID> R2dbcRepository<T, ID>.findByIdOrThrow(id: ID, message: String? = null): Mono<T> {
    return this.findById(id)
               .switchIfEmpty { notFound(message?.let{ it } ?: "Id [$id]로 조회된 정보가 없습니다.") }

}