package com.casadetasha.kexp.petals.processor.post.tests.accessor

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.casadetasha.kexp.petals.PetalTables
import com.casadetasha.kexp.petals.accessor.OptionalNestedPetalClass
import com.casadetasha.kexp.petals.accessor.OptionalParentPetalClass
import com.casadetasha.kexp.petals.migration.`TableMigrations$nested_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$parent_petal`
import com.casadetasha.kexp.petals.processor.post.countMilliseconds
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorOptionalNestedPetalTest : ContainerizedTestBase() {

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
    fun `allows creating a petal without a dependency`() {
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter"
        )

        assertThat(parentPetal.nestedPetal).isNull()
        assertThat(parentPetal.nestedPetalId).isNull()
    }

    @Test
    fun `allows creating a petal without a dependency stores petal in db`() {
        val id = OptionalParentPetalClass.create(name = "Parenter").id
        val loadedParentPetal = OptionalParentPetalClass.load(id)!!

        assertThat(loadedParentPetal.nestedPetal).isNull()
        assertThat(loadedParentPetal.nestedPetalId).isNull()
    }

    @Test
    fun `updating from reference to null dependency removes the local dependency`() {
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        )

        parentPetal.nestedPetal = null

        assertThat(parentPetal.nestedPetal).isNull()
        assertThat(parentPetal.nestedPetalId).isNull()
    }

    @Test
    fun `store() with null dependency removes the reference from db`() {
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        )

        parentPetal.apply {
            this.nestedPetal = null
        }.store(updateNestedDependencies = false)

        val loadedParentPetal = OptionalParentPetalClass.load(parentPetal.id)!!
        assertThat(loadedParentPetal.nestedPetal).isNull()
        assertThat(loadedParentPetal.nestedPetalId).isNull()
    }

    // This is not good behavior. If this test fails because we have fixed this, please delete this test and update
    // documentation referencing this behavior. This test was left in place as a reminder to do so.
    @Test
    fun `accessing a deleted nested petal ID will display previous ID if the nested petal is not accessed`() {
        val nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = nestedPetal
        )

        nestedPetal.delete()

        val loadedParentPetal = OptionalParentPetalClass.load(parentPetal.id)!!
        assertThat(loadedParentPetal.nestedPetalId).isNotNull()
    }

    @Test
    fun `accessing a deleted nested petal ID will display null ID after the nested petal is accessed`() {
        val nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = nestedPetal
        )

        nestedPetal.delete()

        val loadedParentPetal = OptionalParentPetalClass.load(parentPetal.id)!!
        assertThat(loadedParentPetal.nestedPetal).isNull()
        assertThat(loadedParentPetal.nestedPetalId).isNull()
    }

    // Below this line should be identical to tests for AccessorNestedPetalTest tests

    @Test
    fun `load() loads nested dependencies`() {
        val parentPetalId = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(
                name = "Nester"
            )
        ).store().id

        val parentPetal = OptionalParentPetalClass.load(parentPetalId)!!
        assertThat(parentPetal.nestedPetal!!.name).isEqualTo("Nester")
    }

    @Test
    fun `loading petals with eagerLoad=false is faster than with eagerLoad=true`() {
        val parentPetalId = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(
                name = "Nester"
            )
        ).store().id

        val lazyLoadTime = countMilliseconds {
            OptionalParentPetalClass.load(parentPetalId, eagerLoad = false)!!
        }

        val eagerLoadTime = countMilliseconds {
            OptionalParentPetalClass.load(parentPetalId, eagerLoad = true)!!
        }

        assertThat(lazyLoadTime).isLessThan(eagerLoadTime)
    }

    @Test
    fun `accessing nested petals with loaded with eagerLoad=true is faster than with eagerLoad=false`() {
        val parentPetalId = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        ).store().id

        val eagerLoadedParentPetal = OptionalParentPetalClass.load(parentPetalId, eagerLoad = true)!!
        val eagerLoadTime = countMilliseconds {
            eagerLoadedParentPetal.nestedPetal
        }

        val lazyLoadedParentPetal = OptionalParentPetalClass.load(parentPetalId, eagerLoad = false)!!
        val lazyLoadTime = countMilliseconds {
            lazyLoadedParentPetal.nestedPetal
        }

        assertThat(eagerLoadTime).isLessThan(lazyLoadTime)
    }

    @Test
    fun `store() with updateNestedDependencies=false does not store nested data`() {
        val nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = nestedPetal
        )

        parentPetal.apply {
            nestedPetal.name = "Updated name"
        }.store(updateNestedDependencies = false)

        val loadedNestedPetal = OptionalNestedPetalClass.load(nestedPetal.id)!!
        assertThat(loadedNestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `store() with updateNestedDependencies=true stores nested data`() {
        val nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = nestedPetal)

        parentPetal.apply {
            this.nestedPetal!!.name = "Updated name"
        }.store(updateNestedDependencies = true)

        val loadedNestedPetal = OptionalNestedPetalClass.load(nestedPetal.id)!!
        assertThat(loadedNestedPetal.name).isEqualTo("Updated name")
    }

    @Test
    fun `store() with new nested petal updates reference id`() {
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        )
        val secondNestedPetal = OptionalNestedPetalClass.create(name = "SecondNester")

        parentPetal.apply {
            nestedPetal = secondNestedPetal
        }.store()

        val loadedOptionalParentPetalClass = OptionalParentPetalClass.load(parentPetal.id)!!
        assertThat(loadedOptionalParentPetalClass.nestedPetalId).isEqualTo(secondNestedPetal.id)
    }

    @Test
    fun `store() with new nested petal with updateNestedDependencies=false does not store changes to the newly assigned petal`() {
        val parentPetal = OptionalParentPetalClass.create(
            name = "Parenter",
            nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        )
        val secondNestedPetal = OptionalNestedPetalClass.create(name = "SecondNester")

        secondNestedPetal.name = "RenamedSecondNester"
        parentPetal.apply {
            nestedPetal = secondNestedPetal
        }.store(updateNestedDependencies = false)

        val loadedSecondNestedPetal = OptionalNestedPetalClass.load(secondNestedPetal.id)!!
        assertThat(loadedSecondNestedPetal.name).isEqualTo("SecondNester")
    }

    @Test
    fun `loading parents from @ReferencedBy property loads all parents that reference the child on that field`() {
        val nestedPetal = OptionalNestedPetalClass.create(name = "Nester")
        val createdParents = listOf(
            OptionalParentPetalClass.create(
                name = "Parenter1",
                nestedPetal = nestedPetal),
            OptionalParentPetalClass.create(
                name = "Parenter2",
                nestedPetal = nestedPetal)
        )

        val loadedParents = nestedPetal.loadParents()
        val createdNames = createdParents.map { it.name }
        val loadedNames = loadedParents.map { it.name }

        assertThat(loadedParents.size).isEqualTo(createdParents.size)
        assertThat(loadedNames).isEqualTo(createdNames)
    }
}
