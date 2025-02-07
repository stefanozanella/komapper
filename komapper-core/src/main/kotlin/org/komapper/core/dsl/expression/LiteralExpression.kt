package org.komapper.core.dsl.expression

import kotlin.reflect.KClass

internal data class LiteralExpression<T : Any>(val value: T, private val klass: KClass<T>) :
    ColumnExpression<T, T> {
    override val owner: TableExpression<*>
        get() = throw UnsupportedOperationException()
    override val exteriorClass: KClass<T> = klass
    override val interiorClass: KClass<T> = klass
    override val wrap: (T) -> T get() = { it }
    override val unwrap: (T) -> T get() = { it }
    override val columnName: String
        get() = throw UnsupportedOperationException()
    override val alwaysQuote: Boolean get() = false
    override val masking: Boolean get() = false
}
