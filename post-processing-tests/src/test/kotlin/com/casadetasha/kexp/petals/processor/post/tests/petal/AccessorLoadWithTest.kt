package com.casadetasha.kexp.petals.processor.post.tests.petal

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.PartiallyDefaultValuePetalEntity
import com.casadetasha.kexp.petals.accessor.BasicPetal
import com.casadetasha.kexp.petals.accessor.DefaultValuePetal
import com.casadetasha.kexp.petals.accessor.PartiallyDefaultValuePetal
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$basic_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$default_value_petal`
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorLoadWithTest: ContainerizedTestBase() {

    private val tableMigration: BasePetalMigration = `TableMigrations$default_value_petal`()

    private val tableName: String by lazy {
        tableMigration.tableName
    }

    @BeforeTest
    fun setup() {
        tableMigration.migrateToLatest(datasource)
    }

    @AfterTest
    fun teardown() {
        datasource.connection.use { connection ->
            connection.prepareStatement("DELETE FROM \"$tableName\"").execute()
        }
    }

    @Test
    fun `loading with an expression returns matches if found`() {
        val stringValue = "Testy McStringFriend"

        DefaultValuePetal.create(stringValue = "Different not real test string value").store().id
        val id: UUID = DefaultValuePetal.create(stringValue = stringValue).store().id

        val queryMatches = DefaultValuePetal.loadFromQuery { table -> table.stringValue eq stringValue }
        val match = checkNotNull(queryMatches.firstOrNull()) {
            "Did not find petal where \"stringValue eq '$stringValue'\" in DB"
        }

        assertThat(queryMatches).hasSize(1)
        assertThat(match.id).isEqualTo(id)
    }

    @Test
    fun `loading with an expression returns empty list if no matches found`() {
        DefaultValuePetal.create(stringValue = "Testy mcFrandyStringPerson").store()
        val queryMatches: List<DefaultValuePetal> = DefaultValuePetal.loadFromQuery { table ->
            table.stringValue eq "This wasn't it!"
        }

        assertThat(queryMatches).isEmpty()
    }
}
