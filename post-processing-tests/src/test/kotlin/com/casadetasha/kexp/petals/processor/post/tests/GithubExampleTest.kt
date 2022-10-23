package com.casadetasha.kexp.petals.processor.post.tests

import com.casadetasha.kexp.petals.PetalTables.setupAndMigrateTables
import com.casadetasha.kexp.petals.accessor.ExampleClass
import com.casadetasha.kexp.petals.accessor.NestedPetalClass
import com.casadetasha.kexp.petals.accessor.ParentPetalClass
import com.casadetasha.kexp.petals.data.*
import com.casadetasha.kexp.petals.processor.post.tests.base.ContainerizedTestBase
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class GithubExampleTest: ContainerizedTestBase() {

    @BeforeTest
    fun setup() {
        setupAndMigrateTables(datasource)
    }

    @AfterTest
    fun teardown() {
//        datasource.connection.use { connection ->
//            connection.prepareStatement("DELETE FROM \"$tableName\"").execute()
//        }
    }

    @Test
    fun testThings() {
        val newInstance = ExampleClass.create(name = "name")

        val loadedInstance = ExampleClass.load(newInstance.id)

        loadedInstance!!.name = "New name"
        val updatedInstance = ExampleClass.store(loadedInstance)

        val queriedInstance = ExampleClass.loadFromQuery { table -> table.name eq "New name" }

        ExampleClass.delete(loadedInstance)

        val reloadedInstance = ExampleClass.load(newInstance.id)

        val recreatedInstance: ExampleClass = ExampleClass.create(name = "An even NEWER name!")
        val jsonedInstance = Json.encodeToString(recreatedInstance.asData())
        val endBreakPointChecker = "HA!"
    }


    @Test
    fun testMoreThings() {
        val nestedPetal = NestedPetalClass.create(name = "hola nestie")

        val parentPetal: ParentPetalClass = ParentPetalClass.create(
            name = "My name",
            nestedPetal = nestedPetal
        )

        val loadedParentPetal = NestedPetalClass.load(nestedPetal.id)!!.loadParents().first()
        val loadedNestedPetal = ParentPetalClass.load(parentPetal.id)!!.nestedPetal

        val endBreakPointChecker = "HA!"
    }

}
