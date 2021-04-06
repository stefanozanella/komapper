package org.komapper.core.dsl.element

import org.komapper.core.dsl.expr.PropertyExpression

internal sealed class Operand {
    data class Parameter(val expression: PropertyExpression<*>, val value: Any?) : Operand()
    data class Property(val expression: PropertyExpression<*>) : Operand()
}