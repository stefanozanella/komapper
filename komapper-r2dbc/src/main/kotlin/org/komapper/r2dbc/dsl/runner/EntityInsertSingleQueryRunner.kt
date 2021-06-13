package org.komapper.r2dbc.dsl.runner

import org.komapper.core.Statement
import org.komapper.core.dsl.context.EntityInsertContext
import org.komapper.core.dsl.metamodel.EntityMetamodel
import org.komapper.core.dsl.option.EntityInsertOption
import org.komapper.r2dbc.R2dbcDatabaseConfig

internal class EntityInsertSingleQueryRunner<ENTITY : Any, ID, META : EntityMetamodel<ENTITY, ID, META>>(
    context: EntityInsertContext<ENTITY, ID, META>,
    option: EntityInsertOption,
    private val entity: ENTITY
) : R2dbcQueryRunner<ENTITY> {

    private val support: EntityInsertQueryRunnerSupport<ENTITY, ID, META> = EntityInsertQueryRunnerSupport(context, option)

    override suspend fun run(config: R2dbcDatabaseConfig): ENTITY {
        val newEntity = preInsert(config)
        val (_, generatedKeys) = insert(config, newEntity)
        return postInsert(newEntity, generatedKeys)
    }

    private suspend fun preInsert(config: R2dbcDatabaseConfig): ENTITY {
        return support.preInsert(config, entity)
    }

    private suspend fun insert(config: R2dbcDatabaseConfig, entity: ENTITY): Pair<Int, LongArray> {
        val statement = buildStatement(config, entity)
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

    override fun dryRun(config: R2dbcDatabaseConfig): String {
        val statement = buildStatement(config, entity)
        return statement.toSql()
    }

    private fun buildStatement(config: R2dbcDatabaseConfig, entity: ENTITY): Statement {
        return support.buildStatement(config, listOf(entity))
    }
}
