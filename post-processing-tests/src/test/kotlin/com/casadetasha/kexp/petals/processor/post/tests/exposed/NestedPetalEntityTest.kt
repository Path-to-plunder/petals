package com.casadetasha.kexp.petals.processor.post.tests.exposed

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.*
import com.casadetasha.kexp.petals.annotations.BasePetalMigration
import com.casadetasha.kexp.petals.migration.*
import com.casadetasha.kexp.petals.processor.post.ktx.runForEach
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class NestedPetalEntityTest : ContainerizedTestBase() {

    private val tableMigrations: Set<BasePetalMigration> = setOf(
        `TableMigrations$parent_petal`(),
        `TableMigrations$nested_petal`(),
        `TableMigrations$int_id_parent_petal`(),
        `TableMigrations$int_id_nested_petal`(),
        `TableMigrations$long_id_parent_petal`(),
        `TableMigrations$long_id_nested_petal`(),
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
    fun `entity can access nested dependencies for UUID key reference`() {
        val parentPetalId = transaction {
            ParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = NestedPetalClassEntity.new {
                    name = "Nester"
                }
            }.id
        }

        val nestedPetal = transaction {
            ParentPetalClassEntity.findById(parentPetalId)!!.nestedPetal
        }
        assertThat(nestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `entity can access nested dependencies for INT key reference`() {
        val parentPetalId = transaction {
            IntIdParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = IntIdNestedPetalClassEntity.new {
                    name = "Nester"
                }
            }.id
        }

        val nestedPetal = transaction {
            IntIdParentPetalClassEntity.findById(parentPetalId)!!.nestedPetal
        }
        assertThat(nestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `entity can access nested dependencies for LONG key reference`() {
        val parentPetalId = transaction {
            LongIdParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = LongIdNestedPetalClassEntity.new {
                    name = "Nester"
                }
            }.id
        }

        val nestedPetal = transaction {
            LongIdParentPetalClassEntity.findById(parentPetalId)!!.nestedPetal
        }
        assertThat(nestedPetal.name).isEqualTo("Nester")
    }

    @Test
    fun `updating nested petal is reflected when loading parent petal again`() {
        val parentPetal = transaction {
            ParentPetalClassEntity.new {
                name = "Parenter"
                nestedPetal = NestedPetalClassEntity.new { name = "Nester" }
            }
        }

        val secondNestedPetal = transaction {
            val secondNestedPetal = NestedPetalClassEntity.new { name = "SecondNester" }
            parentPetal.apply {
                nestedPetal = secondNestedPetal
            }

            secondNestedPetal
        }

        val loadedNestedPetalClassEntity = transaction {
            ParentPetalClassEntity.findById(parentPetal.id)!!
                .nestedPetal
        }

        assertThat(loadedNestedPetalClassEntity.id).isEqualTo(secondNestedPetal.id)
    }

    @Test
    fun `loading parents from @ReferencedBy property loads all parents that reference the child on that field`() {
        val nestedPetal = transaction { NestedPetalClassEntity.new { name = "Nester" } }
        val createdParents = transaction {
            listOf(
                ParentPetalClassEntity.new {
                    name = "Parenter1"
                    this.nestedPetal = nestedPetal
                },
                ParentPetalClassEntity.new {
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
