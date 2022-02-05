package com.casadetasha.kexp.petals.processor.post.base

import assertk.assertThat
import assertk.assertions.*
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.ClassRule
import org.junit.rules.ExternalResource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

open class ContainerizedTestBase {

    companion object {

        lateinit var datasource: HikariDataSource

        var dbContainer: JdbcDatabaseContainer<*> = PostgreSQLContainer("postgres")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_password")

        @ClassRule
        @JvmField
        val resource: ExternalResource = object : ExternalResource() {

            override fun before() {
                dbContainer.start()
                runMigrations()
            }

            private fun runMigrations() {
                datasource = HikariDataSource(
                    HikariConfig().apply {
                        jdbcUrl = dbContainer.jdbcUrl;
                        username = dbContainer.username;
                        password = dbContainer.password;
                        driverClassName = dbContainer.driverClassName;
                    })

                Database.connect(datasource)
            }

            override fun after() {
                dbContainer.close()
            }
        }
    }
}
