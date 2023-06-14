package io.basquiat.musicshop.common.extensions

import io.basquiat.musicshop.common.utils.notFound
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

suspend fun <T, ID> CoroutineCrudRepository<T, ID>.findByIdOrThrow(id: ID, message: String? = null): T {
    return this.findById(id) ?: notFound(message)
}