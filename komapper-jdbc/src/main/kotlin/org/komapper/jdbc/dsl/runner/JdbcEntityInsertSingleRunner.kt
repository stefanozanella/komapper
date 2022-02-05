package org.komapper.jdbc.dsl.runner

import org.komapper.core.DatabaseConfig
import org.komapper.core.DryRunStatement
import org.komapper.core.dsl.context.EntityInsertContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.runner.EntityInsertSingleRunner
import org.komapper.jdbc.JdbcDatabaseConfig

internal class JdbcEntityInsertSingleRunner<ENTITY : Any, ID : Any, META : EntityMetamodel<ENTITY, ID, META>>(
    context: EntityInsertContext<ENTITY, ID, META>,
    private val entity: ENTITY
) : JdbcRunner<ENTITY> {

    private val runner: EntityInsertSingleRunner<ENTITY, ID, META> =
        EntityInsertSingleRunner(context, entity)

    private val support: JdbcEntityInsertRunnerSupport<ENTITY, ID, META> =
        JdbcEntityInsertRunnerSupport(context)

    override fun run(config: JdbcDatabaseConfig): ENTITY {
        val newEntity = preInsert(config)
        val (_, generatedKeys) = insert(config, newEntity)
        return postInsert(newEntity, generatedKeys)
    }

    private fun preInsert(config: JdbcDatabaseConfig): ENTITY {
        return support.preInsert(config, entity)
    }

    private fun insert(config: JdbcDatabaseConfig, entity: ENTITY): Pair<Int, LongArray> {
        val statement = runner.buildStatement(config, entity)
        return support.insert(config) { it.executeUpdate(statement) }
    }

    private fun postInsert(entity: ENTITY, generatedKeys: LongArray): ENTITY {
        val key = generatedKeys.firstOrNull()
        return if (key != null) {
            support.postInsert(entity, key)
        } else {
            entity
        }
    }

    override fun dryRun(config: DatabaseConfig): DryRunStatement {
        return runner.dryRun(config)
    }
}