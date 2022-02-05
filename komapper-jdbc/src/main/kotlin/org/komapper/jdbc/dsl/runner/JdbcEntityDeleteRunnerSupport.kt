package org.komapper.jdbc.dsl.runner

import org.komapper.core.dsl.context.EntityDeleteContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.runner.checkOptimisticLock
import org.komapper.jdbc.JdbcDatabaseConfig
import org.komapper.jdbc.JdbcExecutor

internal class JdbcEntityDeleteRunnerSupport<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    val context: EntityDeleteContext<ENTITY, ID, META>,
) {

    fun <T> delete(config: JdbcDatabaseConfig, execute: (JdbcExecutor) -> T): T {
        val executor = JdbcExecutor(config, context.options)
        return execute(executor)
    }

    fun postDelete(count: Int, index: Int? = null) {
        if (context.target.versionProperty() != null) {
            checkOptimisticLock(context.options, count, index)
        }
    }
}