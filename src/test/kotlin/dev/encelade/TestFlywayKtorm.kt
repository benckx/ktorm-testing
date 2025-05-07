package dev.encelade

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.ktorm.database.Database
import org.ktorm.dsl.*
import org.ktorm.entity.Entity
import org.ktorm.schema.*
import java.time.Instant
import java.time.LocalDate

class TestFlywayKtorm {

    // type mapping
    object Persons : Table<Person>("person") {
        val id = int("id").primaryKey()
        val firstName = varchar("first_name")
        val dateOfBirth = date("date_of_birth")
        val addedAt = timestamp("added_at")
    }

    // entity
    interface Person : Entity<Person> {
        companion object : Entity.Factory<Person>()

        val id: Int
        var firstName: String
        val dateOfBirth: LocalDate
        val addedAt: Instant
    }

    @Test
    fun evaluate() {
        executeFlyway()

        // NB: one way to use named parameters with a Java class
        val hikariConfig = HikariConfig().apply {
            driverClassName = "org.h2.Driver"
            jdbcUrl = DB_HOST
            username = DB_USER
            password = DB_PASSWORD
            maximumPoolSize = 10
        }

        val database = Database.connect(HikariDataSource(hikariConfig))

        // insertion
        database.insert(Persons) {
            set(Persons.firstName, "John")
            set(Persons.dateOfBirth, LocalDate.of(1990, 1, 1))
        }

        database.insert(Persons) {
            set(Persons.firstName, "Ben")
            set(Persons.dateOfBirth, LocalDate.of(1985, 6, 1))
        }

        // transactions
        database.useTransaction {
            database.insert(Persons) {
                set(Persons.firstName, "Alice")
                set(Persons.dateOfBirth, LocalDate.of(1992, 3, 1))
            }

            database.insert(Persons) {
                set(Persons.firstName, "Bob")
                set(Persons.dateOfBirth, LocalDate.of(1988, 7, 1))
            }
        }

        // select count()
        for (row in database.from(Persons).select(count())) {
            val count = row.getInt(1)
            println("count: $count")
        }

        val count = database
            .from(Persons)
            .select(count())
            .map { it.getInt(1) }
            .first()

        println("count: $count")

        // select
        for (row in database.from(Persons).select()) {
            val id = row[Persons.id]
            val firstName = row[Persons.firstName]
            val dateOfBirth = row[Persons.dateOfBirth]
            val addedAt = row[Persons.addedAt]

            println("$id, first name: $firstName, date of birth: $dateOfBirth, added: $addedAt")
        }

        // TODO: sequence


        // plain SQL
        database.useConnection { connection ->
            connection.prepareStatement("select * from person where first_name = 'Alice'").use { statement ->
                val resultSet = statement.executeQuery()
                while (resultSet.next()) {
                    val id = resultSet.getInt("id")
                    val firstName = resultSet.getString("first_name")
                    val dateOfBirth = resultSet.getDate("date_of_birth")
                    val addedAt = resultSet.getTimestamp("added_at")

                    println("$id, first name: $firstName, date of birth: $dateOfBirth, added: $addedAt")
                }
            }
        }

        // date of birth of Alice
        val aliceDateOfBirth = database.from(Persons)
            .select()
            .where { Persons.firstName eq "Alice" }
            .map { it[Persons.dateOfBirth] }
            .firstOrNull()

        println("Alice's date of birth: $aliceDateOfBirth")
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
