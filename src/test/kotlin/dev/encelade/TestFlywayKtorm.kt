package dev.encelade

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test

class TestFlywayKtorm {

    @Test
    fun evaluate() {
        executeFlyway()
    }

    private fun executeFlyway() {
        val flyway = Flyway.configure()
            .dataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "")
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

}
