package io.basquiat.musicshop.common.extensions

import io.basquiat.musicshop.common.utils.notFound
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.switchIfEmpty

fun <T, ID> ReactiveCrudRepository<T, ID>.findByIdOrThrow(id: ID): Mono<T> {
    return this.findById(id)
               .switchIfEmpty { notFound("Id [$id]로 조회된 정보가 없습니다.") }

}