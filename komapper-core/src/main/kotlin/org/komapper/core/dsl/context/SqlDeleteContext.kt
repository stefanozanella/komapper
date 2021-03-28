package org.komapper.core.dsl.context

import org.komapper.core.dsl.data.Criterion
import org.komapper.core.metamodel.EntityMetamodel

internal data class SqlDeleteContext<ENTITY>(
    val entityMetamodel: EntityMetamodel<ENTITY>,
    val where: List<Criterion> = listOf()
) : Context<ENTITY> {

    override fun getReferencingEntityMetamodels(): List<EntityMetamodel<*>> {
        return listOf(entityMetamodel)
    }

    fun addWhere(where: List<Criterion>): SqlDeleteContext<ENTITY> {
        return copy(where = this.where + where)
    }
}