package org.komapper.tx.r2dbc

import io.r2dbc.spi.ConnectionFactories
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.runBlocking
import org.komapper.core.DefaultLoggerFacade
import org.komapper.core.StdOutLogger
import org.komapper.tx.core.TransactionProperty
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class R2dbcCoroutineTransactionOperatorTest {

    data class Address(val id: Int, val street: String, val version: Int)

    class Repository(private val txManager: R2dbcTransactionManager) {
        suspend fun selectAll(): List<Address> {
            val con = txManager.getConnection()
            val stmt = con.createStatement("select address_id, street, version from address order by address_id")
            val result = stmt.execute().awaitSingle()
            val flow = result.map { row, _ ->
                val id = row.get(0) as Int
                val street = row.get(1) as String
                val version = row.get(2) as Int
                Address(id, street, version)
            }.asFlow()
            return flow.toList().also {
                con.close().awaitFirstOrNull()
            }
        }

        suspend fun selectById(id: Int): Address? {
            return selectAll().firstOrNull { it.id == id }
        }

        suspend fun delete(id: Int): Int {
            val con = txManager.getConnection()
            val stmt = con.createStatement("delete from address where address_id = ?")
            stmt.bind(0, id)
            val result = stmt.execute().awaitSingle()
            return result.rowsUpdated.awaitSingle().also {
                con.close().awaitFirstOrNull()
            }
        }
    }

    private val connectionFactory = ConnectionFactories.get("r2dbc:h2:mem:///transaction-test;DB_CLOSE_DELAY=-1")
    private val txManager = R2dbcTransactionManagerImpl(connectionFactory, DefaultLoggerFacade(StdOutLogger()))
    private val tx = R2dbcCoroutineTransactionOperator(txManager)
    private val repository = Repository(txManager)

    @BeforeTest
    fun before() {
        val sql = """
            CREATE TABLE ADDRESS(ADDRESS_ID INTEGER NOT NULL PRIMARY KEY, STREET VARCHAR(20) UNIQUE, VERSION INTEGER);
            INSERT INTO ADDRESS VALUES(1,'STREET 1',1);
            INSERT INTO ADDRESS VALUES(2,'STREET 2',1);
            INSERT INTO ADDRESS VALUES(3,'STREET 3',1);
            INSERT INTO ADDRESS VALUES(4,'STREET 4',1);
            INSERT INTO ADDRESS VALUES(5,'STREET 5',1);
            INSERT INTO ADDRESS VALUES(6,'STREET 6',1);
            INSERT INTO ADDRESS VALUES(7,'STREET 7',1);
            INSERT INTO ADDRESS VALUES(8,'STREET 8',1);
            INSERT INTO ADDRESS VALUES(9,'STREET 9',1);
            INSERT INTO ADDRESS VALUES(10,'STREET 10',1);
            INSERT INTO ADDRESS VALUES(11,'STREET 11',1);
            INSERT INTO ADDRESS VALUES(12,'STREET 12',1);
            INSERT INTO ADDRESS VALUES(13,'STREET 13',1);
            INSERT INTO ADDRESS VALUES(14,'STREET 14',1);
            INSERT INTO ADDRESS VALUES(15,'STREET 15',1);
        """.trimIndent()

        runBlocking {
            val con = connectionFactory.create().awaitSingle()
            val batch = con.createBatch()
            for (each in sql.split(";")) {
                batch.add(each.trim())
            }
            batch.execute().awaitLast()
            con.close().awaitFirstOrNull()
        }
    }

    @AfterTest
    fun after() {
        val sql = "DROP ALL OBJECTS"
        runBlocking {
            val con = connectionFactory.create().awaitSingle()
            val statement = con.createStatement(sql)
            val result = statement.execute().awaitSingle()
            result.rowsUpdated.awaitSingle()
            con.close().awaitFirstOrNull()
        }
    }

    @Test
    fun select() = runBlocking {
        val list = tx.required {
            repository.selectAll()
        }
        assertEquals(15, list.size)
        assertEquals(Address(1, "STREET 1", 1), list[0])
    }

    @Test
    fun commit() = runBlocking {
        tx.required {
            repository.delete(15)
        }
        tx.required {
            val address = repository.selectById(15)
            assertNull(address)
        }
    }

    @Test
    fun rollback() = runBlocking {
        try {
            tx.required {
                repository.delete(15)
                throw Exception()
            }
        } catch (ignored: Exception) {
        }
        tx.required {
            val address = repository.selectById(15)
            assertNotNull(address)
        }
    }.let { }

    @Test
    fun setRollbackOnly() = runBlocking {
        tx.required { tx ->
            repository.delete(15)
            assertFalse(tx.isRollbackOnly())
            tx.setRollbackOnly()
            assertTrue(tx.isRollbackOnly())
        }
        tx.required {
            val address = repository.selectById(15)
            assertNotNull(address)
        }
    }.let { }

    @Test
    fun isolationLevel() = runBlocking {
        tx.required(transactionProperty = TransactionProperty.IsolationLevel.SERIALIZABLE) {
            repository.delete(15)
        }
        tx.required {
            val address = repository.selectById(15)
            assertNull(address)
        }
    }

    @Test
    fun required_required() = runBlocking {
        tx.required {
            repository.delete(15)
            tx.required {
                val address = repository.selectById(15)
                assertNull(address)
            }
        }
        tx.required {
            val address = repository.selectById(15)
            assertNull(address)
        }
    }

    @Test
    fun requiresNew() = runBlocking {
        tx.requiresNew {
            repository.delete(15)
            val address = repository.selectById(15)
            assertNull(address)
        }
        tx.required {
            val address = repository.selectById(15)
            assertNull(address)
        }
    }

    @Test
    fun required_requiresNew() = runBlocking {
        tx.required { tx ->
            repository.delete(15)
            val address = repository.selectById(15)
            assertNull(address)
            tx.requiresNew {
                val address2 = repository.selectById(15)
                assertNotNull(address2)
            }
        }
        tx.required {
            val address = repository.selectById(15)
            assertNull(address)
        }
    }
}