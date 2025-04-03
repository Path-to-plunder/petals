package com.casadetasha.kexp.petals.processor.post.tests.petal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isLessThan
import com.casadetasha.kexp.petals.accessor.NestedPetalOneToManyClass
import com.casadetasha.kexp.petals.accessor.ParentPetalClass
import com.casadetasha.kexp.petals.accessor.ParentPetalOneToManyClass
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$nested_petal_one_to_many`
import com.casadetasha.kexp.petals.migration.`TableMigrations$parent_petal_one_to_many`
import com.casadetasha.kexp.petals.processor.post.ParentPetalOneToMany
import com.casadetasha.kexp.petals.processor.post.countMilliseconds
import com.casadetasha.kexp.petals.processor.post.ktx.runForEach
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccessorNestedPetalOneToManyTest : ContainerizedTestBase() {

    private val tableMigrations: Set<BasePetalMigration> = setOf(
        `TableMigrations$parent_petal_one_to_many`(),
        `TableMigrations$nested_petal_one_to_many`()
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
        val parentPetal = ParentPetalOneToManyClass.create(
            name = "Parenter",
        )

        val nestedPetalOne = NestedPetalOneToManyClass.create(
            name = "Nester1",
            parentPetal = parentPetal
        )

        val nestedPetalTwo = NestedPetalOneToManyClass.create(
            name = "Nester2",
            parentPetal = parentPetal
        )

        val reloadedParentPetal = ParentPetalOneToManyClass.load(parentPetal.id)!!
        val nestedPetals = reloadedParentPetal.loadNestedPetals()
        assertThat(nestedPetals.size).isEqualTo(2)
        assertThat(nestedPetals[0].name).isEqualTo("Nester1")
        assertThat(nestedPetals[1].name).isEqualTo("Nester2")
    }

}
