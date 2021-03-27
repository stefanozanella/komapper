package org.komapper.core.tx

import org.komapper.core.Scope

@Scope
interface TransactionScope {
    fun <R> required(
        isolationLevel: TransactionIsolationLevel? = null,
        block: TransactionScope.() -> R
    ): R

    fun <R> requiresNew(
        isolationLevel: TransactionIsolationLevel? = null,
        block: TransactionScope.() -> R
    ): R

    fun setRollbackOnly()
    fun isRollbackOnly(): Boolean
}
