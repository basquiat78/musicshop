package io.basquiat.musicshop.common.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class Transaction(
    transactionalOperator: TransactionalOperator
) {
    init {
        Companion.transactionalOperator = transactionalOperator
    }
    companion object {
        lateinit var transactionalOperator: TransactionalOperator
        fun <T> withRollback(publisher: Mono<T>): Mono<T> {
            return transactionalOperator.execute { tx ->
                tx.setRollbackOnly()
                publisher
            }.next()
        }
        fun <T> withRollback(publisher: Flux<T>): Flux<T> {
            return transactionalOperator.execute { tx ->
                tx.setRollbackOnly()
                publisher
            }
        }
    }
}