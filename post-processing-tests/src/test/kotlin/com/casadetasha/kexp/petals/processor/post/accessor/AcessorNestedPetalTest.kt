package com.casadetasha.kexp.petals.processor.post.accessor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.ParentPetalClassEntity
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.accessor.NestedPetalClass
import com.casadetasha.kexp.petals.accessor.ParentPetalClass
import com.casadetasha.kexp.petals.migration.`TableMigrations$nested_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$parent_petal`
import com.casadetasha.kexp.petals.processor.post.base.ContainerizedTestBase
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.transactions.transaction
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
    fun `load() loads nested data`() {
        val parentPetalId = ParentPetalClass.create(
            nestedPetal = NestedPetalClass.create(
                name = "Nester"
            )
        ).store().id

        val parentPetal = ParentPetalClass.load(parentPetalId)!!
        transaction {
            assertThat(parentPetal.nestedPetal.name).isEqualTo("Nester")
        }
    }

    @Test
    fun `store() without updateNestedDependencies does not store nested data`() {
        val nestedPetal = NestedPetalClass.create(name = "Nester")
        val parentPetal = ParentPetalClass.create(nestedPetal = nestedPetal)

        parentPetal.apply {
            nestedPetal.name = "Updated name"
        }.store(updateNestedDependencies = false)

        val loadedNestedPetal = NestedPetalClass.load(nestedPetal.id)!!
        assertThat(loadedNestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `store() with updateNestedDependencies does stores nested data`() {
        val nestedPetal = NestedPetalClass.create(name = "Nester")
        val parentPetal = ParentPetalClass.create(nestedPetal = nestedPetal)

        parentPetal.applyInsideTransaction {
            this.nestedPetal.name = "Updated name"
        }.store(updateNestedDependencies = true)

        val loadedNestedPetal = NestedPetalClass.load(nestedPetal.id)!!
        assertThat(loadedNestedPetal.name).isEqualTo("Updated name")
    }

    @Test
    fun `store() with new nested petal updates reference id`() {
        val parentPetal = ParentPetalClass.create(
            nestedPetal = NestedPetalClass.create(name = "Nester")
        )
        val secondNestedPetal = NestedPetalClass.create(name = "SecondNester")

        parentPetal.apply {
            nestedPetal = secondNestedPetal
        }.store()

        val loadedParentPetalClass = ParentPetalClass.load(parentPetal.id)!!
        assertThat(loadedParentPetalClass.nestedPetalId).isEqualTo(secondNestedPetal.id)
    }

    @Test
    fun `store() with new nested petal without updateNestedDependencies does not store changes to the newly assigned petal`() {
        val parentPetal = ParentPetalClass.create(
            nestedPetal = NestedPetalClass.create(name = "Nester")
        )
        val secondNestedPetal = NestedPetalClass.create(name = "SecondNester")

        secondNestedPetal.name = "RenamedSecondNester"
        parentPetal.apply {
            nestedPetal = secondNestedPetal
        }.store()

        val loadedSecondNestedPetal = NestedPetalClass.load(secondNestedPetal.id)!!
        assertThat(loadedSecondNestedPetal.name).isEqualTo("SecondNester")
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
