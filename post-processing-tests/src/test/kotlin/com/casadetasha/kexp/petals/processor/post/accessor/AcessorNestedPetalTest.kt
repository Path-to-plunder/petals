package com.casadetasha.kexp.petals.processor.post.accessor.exposed

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.accessor.NestedPetalClass
import com.casadetasha.kexp.petals.accessor.ParentPetalClass
import com.casadetasha.kexp.petals.migration.`TableMigrations$nested_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$parent_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import java.util.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorNestedPetalTest: ContainerizedTestBase() {

    private val tableNames: Set<String> by lazy {
        setOf(
            `TableMigrations$parent_petal`().tableName,
            `TableMigrations$nested_petal`().tableName
        )
    }

    @BeforeTest
    fun setup() {
        PetalTables.setupAndMigrateTables(datasource)
    }

    @AfterTest
    fun teardown() {
        datasource.connection.use { connection ->
            tableNames.forEach { tableName ->
                connection.prepareStatement("DELETE FROM \"$tableName\"").execute()
            }
        }
    }

    @Test
    fun `Exports to accessor`() {
    }

    @Test
    fun `load() loads stored data`() {
        val parentPetalId = ParentPetalClass(
            nestedPetal = NestedPetalClass(
                name = "Nester"
            )
        ).store().id

        val parentPetal = ParentPetalClass.load(parentPetalId)!!
        assertThat(parentPetal.nestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `store() without provided ID stores data with generated ID`() {
    }

    @Test
    fun `store() with provided ID stores data`() {
    }

    @Test
    fun `changing values on loaded data without calling store() does not update data`() {
    }

    @Test
    fun `calling store() after changing values on loaded data updates data`() {
    }

    @Test
    fun `calling delete() on stored data deletes the data`() {
    }

    @Test
    fun `calling delete() on non stored data does not crash`() {
    }
}
