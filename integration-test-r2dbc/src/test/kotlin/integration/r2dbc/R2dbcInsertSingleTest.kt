package integration.r2dbc

import integration.core.Address
import integration.core.Dbms
import integration.core.Department
import integration.core.Human
import integration.core.IdentityStrategy
import integration.core.Person
import integration.core.Run
import integration.core.SequenceStrategy
import integration.core.address
import integration.core.department
import integration.core.human
import integration.core.identityStrategy
import integration.core.person
import integration.core.sequenceStrategy
import org.junit.jupiter.api.extension.ExtendWith
import org.komapper.core.ClockProvider
import org.komapper.core.UniqueConstraintException
import org.komapper.core.dsl.Meta
import org.komapper.core.dsl.QueryDsl
import org.komapper.core.dsl.operator.concat
import org.komapper.core.dsl.query.first
import org.komapper.r2dbc.R2dbcDatabase
import org.komapper.r2dbc.R2dbcDatabaseConfig
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@ExtendWith(R2dbcEnv::class)
class R2dbcInsertSingleTest(private val db: R2dbcDatabase) {

    @Test
    fun test() = inTransaction(db) {
        val a = Meta.address
        val address = Address(16, "STREET 16", 0)
        db.runQuery { QueryDsl.insert(a).single(address) }
        val address2 = db.runQuery {
            QueryDsl.from(a).where {
                a.addressId eq 16
            }.first()
        }
        assertEquals(address, address2)
    }

    @Test
    fun createdAt_localDateTime() = inTransaction(db) {
        val p = Meta.person
        val person1 = Person(1, "ABC")
        val id = db.runQuery { QueryDsl.insert(p).single(person1) }.personId
        val person2 = db.runQuery { QueryDsl.from(p).where { p.personId eq id }.first() }
        assertNotNull(person2.createdAt)
        assertNotNull(person2.updatedAt)
        assertEquals(person2.createdAt, person2.updatedAt)
        val person3 = db.runQuery {
            QueryDsl.from(p).where {
                p.personId to 1
            }.first()
        }
        assertEquals(person2, person3)
    }

    @Run(unless = [Dbms.MARIADB, Dbms.POSTGRESQL, Dbms.SQLSERVER])
    @Test
    fun createdAt_offsetDateTime() = inTransaction(db) {
        val h = Meta.human
        val human1 = Human(1, "ABC")
        val id = db.runQuery { QueryDsl.insert(h).single(human1) }.humanId
        val human2 = db.runQuery { QueryDsl.from(h).where { h.humanId eq id }.first() }
        assertNotNull(human2.createdAt)
        assertNotNull(human2.updatedAt)
        assertEquals(human2.createdAt, human2.updatedAt)
        val human3 = db.runQuery {
            QueryDsl.from(h).where {
                h.humanId to 1
            }.first()
        }
        assertEquals(human2, human3)
    }

    @Test
    fun createdAt_customize() = inTransaction(db) {
        val instant = Instant.parse("2021-01-01T00:00:00Z")
        val zoneId = ZoneId.of("UTC")

        val p = Meta.person
        val config = object : R2dbcDatabaseConfig by db.config {
            override val clockProvider = ClockProvider {
                Clock.fixed(instant, zoneId)
            }
        }
        val myDb = R2dbcDatabase.create(config)
        val person1 = Person(1, "ABC")
        val id = myDb.runQuery { QueryDsl.insert(p).single(person1) }
        val person2 = db.runQuery {
            QueryDsl.from(p).where {
                p.personId to id
            }.first()
        }
        assertNotNull(person2.createdAt)
        assertNotNull(person2.updatedAt)
        assertEquals(person2.createdAt, person2.updatedAt)
        assertEquals(LocalDateTime.ofInstant(instant, zoneId), person2.createdAt)
    }

    @Test
    fun uniqueConstraintException() = inTransaction(db) {
        val a = Meta.address
        val address = Address(1, "STREET 1", 0)
        assertFailsWith<UniqueConstraintException> {
            db.runQuery { QueryDsl.insert(a).single(address) }.let { }
        }
    }

    @Test
    fun identityGenerator() = inTransaction(db) {
        for (i in 1..201) {
            val m = Meta.identityStrategy
            val strategy = IdentityStrategy(0, "test")
            val result = db.runQuery { QueryDsl.insert(m).single(strategy) }
            assertEquals(i, result.id)
        }
    }

    @Run(unless = [Dbms.MYSQL])
    @Test
    fun sequenceGenerator() = inTransaction(db) {
        for (i in 1..201) {
            val m = Meta.sequenceStrategy
            val strategy = SequenceStrategy(0, "test")
            val result = db.runQuery { QueryDsl.insert(m).single(strategy) }
            assertEquals(i, result.id)
        }
    }

    @Run(unless = [Dbms.MYSQL])
    @Test
    fun sequenceGenerator_disableSequenceAssignment() = inTransaction(db) {
        val m = Meta.sequenceStrategy
        val strategy = SequenceStrategy(50, "test")
        val result = db.runQuery {
            QueryDsl.insert(m).single(strategy).options {
                it.copy(disableSequenceAssignment = true)
            }
        }
        assertEquals(50, result.id)
    }

    @Test
    fun onDuplicateKeyUpdate_insert() = inTransaction(db) {
        val d = Meta.department
        val department = Department(5, 50, "PLANNING", "TOKYO", 0)
        val query = QueryDsl.insert(d).onDuplicateKeyUpdate().single(department)
        db.runQuery { query }
        val found = db.runQuery { QueryDsl.from(d).where { d.departmentId eq 5 }.first() }
        assertNotNull(found)
    }

    @Test
    fun onDuplicateKeyUpdateWithKeys_insert() = inTransaction(db) {
        val d = Meta.department
        val department = Department(5, 50, "PLANNING", "TOKYO", 0)
        val query = QueryDsl.insert(d).onDuplicateKeyUpdate(d.departmentNo).single(department)
        db.runQuery { query }
        val found = db.runQuery { QueryDsl.from(d).where { d.departmentId eq 5 }.first() }
        assertNotNull(found)
    }

    @Test
    fun onDuplicateKeyUpdate_update() = inTransaction(db) {
        val d = Meta.department
        val department = Department(1, 50, "PLANNING", "TOKYO", 10)
        val query = QueryDsl.insert(d).onDuplicateKeyUpdate().single(department)
        db.runQuery { query }
        val found = db.runQuery { QueryDsl.from(d).where { d.departmentId eq 1 }.first() }
        assertEquals(50, found.departmentNo)
        assertEquals("PLANNING", found.departmentName)
        assertEquals("TOKYO", found.location)
        assertEquals(10, found.version)
    }

    @Test
    fun onDuplicateKeyUpdateWithKeys_update() = inTransaction(db) {
        val d = Meta.department
        val department = Department(6, 10, "PLANNING", "TOKYO", 10)
        val query = QueryDsl.insert(d).onDuplicateKeyUpdate(d.departmentNo).single(department)
        db.runQuery { query }
        val found = db.runQuery { QueryDsl.from(d).where { d.departmentNo eq 10 }.first() }
        assertEquals(1, found.departmentId)
        assertEquals(10, found.departmentNo)
        assertEquals("PLANNING", found.departmentName)
        assertEquals("TOKYO", found.location)
        assertEquals(10, found.version)
    }

    @Test
    @Run(unless = [Dbms.MARIADB])
    fun onDuplicateKeyUpdate_update_set() = inTransaction(db) {
        val d = Meta.department
        val department = Department(1, 50, "PLANNING", "TOKYO", 10)
        val query = QueryDsl.insert(d).onDuplicateKeyUpdate().set { excluded ->
            d.departmentName eq "PLANNING2"
            d.location eq concat(d.location, concat("_", excluded.location))
        }.single(department)
        db.runQuery { query }
        val found = db.runQuery { QueryDsl.from(d).where { d.departmentId eq 1 }.first() }
        assertEquals(10, found.departmentNo)
        assertEquals("PLANNING2", found.departmentName)
        assertEquals("NEW YORK_TOKYO", found.location)
        assertEquals(1, found.version)
    }

    @Test
    @Run(unless = [Dbms.MARIADB])
    fun onDuplicateKeyUpdateWithKey_update_set() = inTransaction(db) {
        val d = Meta.department
        val department = Department(5, 10, "PLANNING", "TOKYO", 10)
        val query = QueryDsl.insert(d)
            .onDuplicateKeyUpdate(d.departmentNo)
            .set { excluded ->
                d.departmentName eq "PLANNING2"
                d.location eq concat(d.location, concat("_", excluded.location))
            }.single(department)
        db.runQuery { query }
        val found = db.runQuery { QueryDsl.from(d).where { d.departmentNo eq 10 }.first() }
        assertEquals(1, found.departmentId)
        assertEquals("PLANNING2", found.departmentName)
        assertEquals("NEW YORK_TOKYO", found.location)
        assertEquals(1, found.version)
    }

    @Test
    fun onDuplicateKeyIgnore_inserted() = inTransaction(db) {
        val a = Meta.address
        val address = Address(16, "STREET 16", 0)
        val query = QueryDsl.insert(a).onDuplicateKeyIgnore().single(address)
        val count = db.runQuery { query }
        assertEquals(1, count)
    }

    @Test
    fun onDuplicateKeyIgnore_ignored() = inTransaction(db) {
        val a = Meta.address
        val address = Address(1, "STREET 100", 0)
        val query = QueryDsl.insert(a).onDuplicateKeyIgnore().single(address)
        val count = db.runQuery { query }
        assertEquals(0, count)
    }

    @Test
    fun onDuplicateKeyIgnoreWithKey_ignored() = inTransaction(db) {
        val a = Meta.address
        val address = Address(100, "STREET 1", 0)
        val query = QueryDsl.insert(a).onDuplicateKeyIgnore(a.street).single(address)
        val count = db.runQuery { query }
        assertEquals(0, count)
    }
}