package dev.encelade

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.count
import org.ktorm.dsl.from
import org.ktorm.dsl.insert
import org.ktorm.dsl.select
import org.ktorm.schema.Table
import org.ktorm.schema.int
import org.ktorm.schema.varchar
import org.ktorm.schema.date
import org.ktorm.schema.datetime
import java.time.LocalDate

class TestFlywayKtorm {

    object Person : Table<Nothing>("person") {
        val id = int("id").primaryKey()
        val firstName = varchar("first_name")
        val dateOfBirth = date("date_of_birth")
        val addedAt = datetime("added_at")
    }

    @Test
    fun evaluate() {
        executeFlyway()

        val database = Database.connect(DB_HOST, user = DB_USER, password = DB_PASSWORD)

        database.insert(Person) {
            set(Person.firstName, "John")
            set(Person.dateOfBirth, LocalDate.of(1990, 1, 1))
        }

        database.insert(Person) {
            set(Person.firstName, "Ben")
            set(Person.dateOfBirth, LocalDate.of(1985, 6, 1))
        }

        for (row in database.from(Person).select(count())) {
            val count = row.getInt(1)
            println("Count: $count")
        }

        for (row in database.from(Person).select()) {
            val id = row[Person.id]
            val firstName = row[Person.firstName]
            val dateOfBirth = row[Person.dateOfBirth]
            val addedAt = row[Person.addedAt]

            println("$id, first name: $firstName, date of birth: $dateOfBirth, added: $addedAt")
        }
    }

    private fun executeFlyway() {
        val flyway = Flyway.configure()
            .dataSource(DB_HOST, DB_USER, DB_PASSWORD)
            .locations("classpath:/migrations")
            .load()

        // log migration info
        val info = flyway.info()
        val migrations = info.all()

        println("Classpath: ${System.getProperty("java.class.path")}")
        println("Flyway Migrations: (${migrations.size})")
        migrations.forEach { migration ->
            println("Version: ${migration.version}, Location: ${migration.physicalLocation}, Description: ${migration.description}, State: ${migration.state}")
        }

        // run migrations
        flyway.migrate()

        println("Flyway migrations executed successfully!")
    }

    private companion object {

        const val DB_HOST = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1"
        const val DB_USER = "sa"
        const val DB_PASSWORD = ""

    }

}
