package com.casadetasha.kexp.petals.processor.post.tests.exposed

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.casadetasha.kexp.petals.OptionalNestedPetalClassEntity
import com.casadetasha.kexp.petals.OptionalParentPetalClassEntity
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.`TableMigrations$optional_nested_petal`
import com.casadetasha.kexp.petals.migration.`TableMigrations$optional_parent_petal`
import com.casadetasha.kexp.petals.processor.post.ktx.runForEach
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class OptionalNestedPetalEntityTest : ContainerizedTestBase() {

    private val tableMigrations: Set<BasePetalMigration> = setOf(
            `TableMigrations$optional_parent_petal`(),
            `TableMigrations$optional_nested_petal`(),
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
    fun `creates a petal without an optional nested dependency`() {
        val id = transaction { OptionalParentPetalClassEntity.new { name = "Parenter" }.id }
        val loadedNestedPetal: OptionalNestedPetalClassEntity? = transaction {
            OptionalParentPetalClassEntity.findById(id)!!.nestedPetal
        }

        assertThat(loadedNestedPetal).isNull()
    }

    @Test
    fun `allows removing an existing nested dependency`() {
        val parentPetal = transaction {
            OptionalParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = OptionalNestedPetalClassEntity.new { name = "Nester" }
            }
        }

        transaction { parentPetal.nestedPetal = null }

        val nestedPetal: OptionalNestedPetalClassEntity? = transaction {
            OptionalParentPetalClassEntity.findById(parentPetal.id)!!.nestedPetal
        }
        assertThat(nestedPetal).isNull()
    }

    @Test
    fun `accessing a deleted nested petal ID will display null ID after the nested petal is accessed`() {
        val nestedPetal = transaction { OptionalNestedPetalClassEntity.new {
            name = "Nester"
        } }
        val parentPetal = transaction {
            OptionalParentPetalClassEntity.new {
                name = "Parenter"
                this.nestedPetal = nestedPetal
            }
        }

        val loadedNestedPetal = transaction {
            nestedPetal.delete()
            OptionalParentPetalClassEntity.findById(parentPetal.id)!!.nestedPetal
        }
        assertThat(loadedNestedPetal).isNull()
    }

    // Below this line should be identical to tests for NestedPetalEntityTest tests with Optional versions

    @Test
    fun `entity can access nested dependencies`() {
        val parentPetalId = transaction {
            OptionalParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = OptionalNestedPetalClassEntity.new {
                    name = "Nester"
                }
            }.id
        }

        val nestedPetal = transaction {
            OptionalParentPetalClassEntity.findById(parentPetalId)!!.nestedPetal!!
        }
        assertThat(nestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `updating nested petal is reflected when loading parent petal again`() {
        val parentPetal = transaction {
            OptionalParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = OptionalNestedPetalClassEntity.new { name = "Nester" }
            }
        }

        val secondNestedPetal = transaction {
            val secondNestedPetal = OptionalNestedPetalClassEntity.new { name = "SecondNester" }
            parentPetal.apply {
                nestedPetal = secondNestedPetal
            }

            secondNestedPetal
        }

        val loadedNestedPetalClassEntity = transaction {
            OptionalParentPetalClassEntity.findById(parentPetal.id)!!
                .nestedPetal!!
        }

        assertThat(loadedNestedPetalClassEntity.id).isEqualTo(secondNestedPetal.id)
    }

    @Test
    fun `loading parents from @ReferencedBy property loads all parents that reference the child on that field`() {
        val nestedPetal = transaction { OptionalNestedPetalClassEntity.new { name = "Nester" } }
        val createdParents = transaction {
            listOf(
                OptionalParentPetalClassEntity.new {
                    name = "Parenter1"
                    this.nestedPetal = nestedPetal
                },
                OptionalParentPetalClassEntity.new {
                    name = "Parenter2"
                    this.nestedPetal = nestedPetal
                }
            )
        }
        val loadedParents = transaction { nestedPetal.parents }

        val createdNames = createdParents.map { it.name }
        val loadedNames = transaction { loadedParents.map { it.name } }

        assertThat(loadedParents.count().toInt()).isEqualTo(createdParents.size)
        assertThat(loadedNames).isEqualTo(createdNames)
    }
}
