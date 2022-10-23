package com.casadetasha.kexp.petals.processor.post.tests.petal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import com.casadetasha.kexp.petals.accessor.NestedPetalClass
import com.casadetasha.kexp.petals.accessor.ParentPetalClass
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$nested_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$parent_petal`
import com.casadetasha.kexp.petals.processor.post.countMilliseconds
import com.casadetasha.kexp.petals.processor.post.ktx.runForEach
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorNestedPetalTest : ContainerizedTestBase() {

    private val tableMigrations: Set<BasePetalMigration> = setOf(
        `TableMigrations$parent_petal`(),
        `TableMigrations$nested_petal`()
    )

    private val tableNames: Set<String> by lazy {
        tableMigrations
            .map { it.tableName }
            .toSet()
    }

    @BeforeTest
    fun setup() {
        tableMigrations.runForEach { migrateToLatest(datasource) }
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
    fun `load() loads nested dependencies`() {
        val parentPetalId = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = NestedPetalClass.create(
                name = "Nester"
            )
        ).id

        val parentPetal = ParentPetalClass.load(parentPetalId)!!
        assertThat(parentPetal.nestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `loading petals with eagerLoad=false is faster than with eagerLoad=true`() {
        val parentPetalId = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = NestedPetalClass.create(
                name = "Nester"
            )
        ).id

        val lazyLoadTime = countMilliseconds {
            ParentPetalClass.load(parentPetalId, eagerLoad = false)!!
        }

        val eagerLoadTime = countMilliseconds {
            ParentPetalClass.load(parentPetalId, eagerLoad = true)!!
        }

        assertThat(lazyLoadTime).isLessThan(eagerLoadTime)
    }

    @Test
    fun `accessing nested petals with loaded with eagerLoad=true is faster than with eagerLoad = false`() {
        val parentPetalId = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = NestedPetalClass.create(
                name = "Nester"
            )
        ).id

        val eagerLoadedParentPetal = ParentPetalClass.load(parentPetalId, eagerLoad = true)!!
        val eagerLoadTime = countMilliseconds {
            eagerLoadedParentPetal.nestedPetal
        }

        val lazyLoadedParentPetal = ParentPetalClass.load(parentPetalId, eagerLoad = false)!!
        val lazyLoadTime = countMilliseconds {
            lazyLoadedParentPetal.nestedPetal
        }

        assertThat(eagerLoadTime).isLessThan(lazyLoadTime)
    }

    @Test
    fun `store() with updateNestedDependencies=false does not store nested data`() {
        val nestedPetal = NestedPetalClass.create(name = "Nester")
        val parentPetal = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = nestedPetal
        )

        parentPetal.apply {
            nestedPetal.name = "Updated name"
            ParentPetalClass.store(this, updateNestedDependencies = false)
        }

        val loadedNestedPetal = NestedPetalClass.load(nestedPetal.id)!!
        assertThat(loadedNestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `store() with updateNestedDependencies=true stores nested data`() {
        val nestedPetal = NestedPetalClass.create(name = "Nester")
        val parentPetal = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = nestedPetal)

        parentPetal.apply {
            this.nestedPetal.name = "Updated name"
            ParentPetalClass.store(this, updateNestedDependencies = true)
        }

        val loadedNestedPetal = NestedPetalClass.load(nestedPetal.id)!!
        assertThat(loadedNestedPetal.name).isEqualTo("Updated name")
    }

    @Test
    fun `store() with new nested petal updates reference id`() {
        val parentPetal = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = NestedPetalClass.create(name = "Nester")
        )
        val secondNestedPetal = NestedPetalClass.create(name = "SecondNester")

        parentPetal.apply {
            nestedPetal = secondNestedPetal
            ParentPetalClass.store(this)
        }

        val loadedParentPetalClass = ParentPetalClass.load(parentPetal.id)!!
        assertThat(loadedParentPetalClass.nestedPetalId).isEqualTo(secondNestedPetal.id)
    }

    @Test
    fun `store() with new nested petal with updateNestedDependencies=false does not store changes to the newly assigned petal`() {
        val parentPetal = ParentPetalClass.create(
            name = "Parenter",
            nestedPetal = NestedPetalClass.create(name = "Nester")
        )
        val secondNestedPetal = NestedPetalClass.create(name = "SecondNester")

        secondNestedPetal.name = "RenamedSecondNester"
        parentPetal.apply {
            nestedPetal = secondNestedPetal
            ParentPetalClass.store(this, updateNestedDependencies = false)
        }

        val loadedSecondNestedPetal = NestedPetalClass.load(secondNestedPetal.id)!!
        assertThat(loadedSecondNestedPetal.name).isEqualTo("SecondNester")
    }

    @Test
    fun `loading parents from @ReferencedBy property loads all parents that reference the child on that field`() {
        val nestedPetal = NestedPetalClass.create(name = "Nester")
        val createdParents = listOf(
            ParentPetalClass.create(
                name = "Parenter1",
                nestedPetal = nestedPetal),
            ParentPetalClass.create(
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
