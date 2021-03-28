package org.komapper.core.dsl.command

import org.komapper.core.DatabaseConfig
import org.komapper.core.data.Statement
import org.komapper.core.jdbc.JdbcExecutor

internal class SqlUpdateCommand(
    config: DatabaseConfig,
    private val statement: Statement
) : Command<Int> {

    private val executor: JdbcExecutor = JdbcExecutor(config)

    override fun execute(): Int {
        return executor.executeUpdate(statement) { _, count ->
            count
        }
    }
}