package org.komapper.core.data

data class Statement(val sql: String, val values: List<Value>, val log: String?) {
    constructor(sql: String) : this(sql, emptyList(), sql)

    override fun toString(): String {
        return sql
    }

    infix operator fun plus(other: Statement): Statement {
        val separator = if (this.sql.trimEnd().endsWith(";")) "" else ";"
        val sql = this.sql + separator + other.sql
        val values = this.values + other.values
        val log = this.log + separator + other.log
        return Statement(sql, values, log)
    }
}
