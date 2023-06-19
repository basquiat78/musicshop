package io.basquiat.musicshop.common.transaction

import org.springframework.stereotype.Component
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.transaction.reactive.executeAndAwait

@Component
class Transaction (
    transactionalOperator: TransactionalOperator
) {
    init {
        Companion.transactionalOperator = transactionalOperator
    }
    companion object {
        lateinit var transactionalOperator: TransactionalOperator
        suspend fun <T, S> withRollback(value: T, receiver: suspend (T) -> S): S {
            return transactionalOperator.executeAndAwait {
                it.setRollbackOnly()
                receiver(value)
            }
        }

        suspend fun withRollback(receiver: suspend () -> Unit) {
            return transactionalOperator.executeAndAwait {
                it.setRollbackOnly()
                receiver()
            }
        }
    }
}