package org.komapper.core.dsl.expression

import kotlin.reflect.KClass

interface TableExpression<T : Any> {
    fun klass(): KClass<T>
    fun tableName(): String
    fun catalogName(): String
    fun schemaName(): String
    fun alwaysQuote(): Boolean
    fun getCanonicalTableName(enquote: (String) -> String): String {
        val transform = if (alwaysQuote()) {
            enquote
        } else {
            { it }
        }
        return listOf(catalogName(), schemaName(), tableName())
            .filter { it.isNotBlank() }.joinToString(".", transform = transform)
    }
}