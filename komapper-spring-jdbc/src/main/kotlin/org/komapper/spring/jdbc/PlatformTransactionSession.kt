package org.komapper.spring.jdbc

import org.komapper.jdbc.JdbcSession
import org.komapper.tx.core.TransactionOperator
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import org.springframework.transaction.PlatformTransactionManager
import java.sql.Connection
import javax.sql.DataSource

class PlatformTransactionSession(transactionManager: PlatformTransactionManager, dataSource: DataSource) :
    JdbcSession {

    private val dataSourceProxy = when (dataSource) {
        is TransactionAwareDataSourceProxy -> dataSource
        else -> TransactionAwareDataSourceProxy(dataSource)
    }

    override val transactionOperator: TransactionOperator = PlatformTransactionOperator(transactionManager)

    override fun getConnection(): Connection {
        return dataSourceProxy.connection
    }
}