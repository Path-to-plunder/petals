package com.casadetasha.kexp.petals.processor.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.fail
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.processor.util.compileSources
import com.tschuchort.compiletesting.SourceFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.Test
import java.lang.reflect.Method

class IdColumnSqlTest {

    @Test
    fun `Creates table with int id`() {
        val petalSchemaMigration = generateSchemaMigrations(intIdPetalSchema, "int_id_petal")
        val migrationSql = petalSchemaMigration[1]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).createsTableWithExactColumns(
            tableName = "int_id_petal",
            columnCreationSql = listOf(
                " id SERIAL PRIMARY KEY,",
                " \"column\" TEXT NOT NULL"
            )
        )
    }

    @Test
    fun `Creates table with long id`() {
        val petalSchemaMigration = generateSchemaMigrations(longIdPetalSchema, "long_id_petal")
        val migrationSql = petalSchemaMigration[1]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).createsTableWithExactColumns(
                tableName = "long_id_petal",
                columnCreationSql = listOf(
                    " id BIGSERIAL PRIMARY KEY,",
                    " \"column\" TEXT NOT NULL"
                )
            )
    }


    @Test
    fun `Creates table with uuid id`() {
        val petalSchemaMigration = generateSchemaMigrations(uuidIdPetalSchema, "uuid_id_petal")
        val migrationSql = petalSchemaMigration[1]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).createsTableWithExactColumns(
                tableName = "uuid_id_petal",
                columnCreationSql = listOf(
                    " id uuid PRIMARY KEY,",
                    " \"column\" TEXT NOT NULL"
                )
            )
    }

    private fun generateSchemaMigrations(sourceFile: SourceFile, tableName: String): Map<Int, PetalSchemaMigration> {
        val compilationResult = compileSources(sourceFile)
        val generatedMigrationClass =
            compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$$tableName")

        val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
        val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
        val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
        val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

        return petalMigration.schemaMigrations
    }

    companion object {

        private val intIdPetalSchema = SourceFile.kotlin(
            "IntIdPetalSchema.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
            
            @Petal(tableName = "int_id_petal", className = "IntIdPetal", primaryKeyType = PetalPrimaryKey.INT)
            interface IntIdPetal

            @PetalSchema(petal = IntIdPetal::class)
            interface IntIdPetalSchema {
                val column: String
            }
            """.trimIndent()
        )

        private val longIdPetalSchema = SourceFile.kotlin(
            "LongIdPetalSchema.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
            
            @Petal(tableName = "long_id_petal", className = "LongIdPetal", primaryKeyType = PetalPrimaryKey.LONG)
            interface LongIdPetal

            @PetalSchema(petal = LongIdPetal::class)
            interface LongIdPetalSchema {
                val column: String
            }
            """.trimIndent()
        )

        private val uuidIdPetalSchema = SourceFile.kotlin(
            "UuidIdPetalSchema.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
            
            @Petal(tableName = "uuid_id_petal", className = "UuidIdPetal", primaryKeyType = PetalPrimaryKey.UUID)
            interface UuidIdPetal

            @PetalSchema(petal = UuidIdPetal::class)
            interface UuidIdPetalSchema {
                val column: String
            }
            """.trimIndent()
        )
    }
}

internal fun List<String>.createsTable(tableName: String) {
    val actual = this

    assertThat(actual).isNotNull()

    val createTableText = "CREATE TABLE \"$tableName\" ("
    val firstRowStartsCreationBlock: Boolean = actual.first() == createTableText
    val lastRowClosesCreationBlock: Boolean = actual.last() == " )"

    if (firstRowStartsCreationBlock && lastRowClosesCreationBlock) {
        return
    }
    if (!firstRowStartsCreationBlock) {
        fail(message = "Table is not created properly, expected = $createTableText, actual = \"${actual.first()}\"")
    }
    fail(message = "Table does not close properly, expected = \" )\", actual = \"${actual.last()}\"")
}


internal fun assertThatSqlList(migrationSql: List<String>) = migrationSql

internal fun List<String>.createsTableWithExactColumns(
    tableName: String, columnCreationSql: List<String>
) {
    val actual = this
    assertThatSqlList(actual).createsTable(tableName)
    assertThatSqlList(actual.subList(1, actual.size - 1)).containsExactColumnMigrations(columnCreationSql)
}

internal fun List<String>.migratesTableWithExactColumnAlterations(tableName: String, columnAlterationSql: List<String>) {
    val actual = this
    assertThatSqlList(actual).migratesTable(tableName)
    assertThatSqlList(actual.subList(1, actual.size)).containsExactColumnMigrations(columnAlterationSql)
}

internal fun List<String>.migratesTable(tableName: String) {
    val actual = this
    assertThat(actual).isNotNull()

    val createTableText = "ALTER TABLE \"$tableName\""
    val firstRowStartsAlterTableBlock: Boolean = actual.first() == createTableText

    if (firstRowStartsAlterTableBlock) { return }
    fail(message = "Table is not created properly, expected = $createTableText, actual = \"${actual.first()}\"")
}

internal fun List<String>.containsExactColumnMigrations(expectedColumnRows: List<String>) {
    val actual = this
    val exactlyOneRowDoesNotEndInComma = actual.filter { it.last() == ',' }.size == actual.size - 1
    val lastRowDoesNotEndInComma = actual.last().last() != ','

    if (!exactlyOneRowDoesNotEndInComma || !lastRowDoesNotEndInComma) {
        fail(message = "SQL parsing issue, all column creation rows except the last must end with ','" +
                " expected = ${expectedColumnRows.map { "'$it'" }}, actual = ${actual.map { "'$it'" }}"
        )
    }

    // containsExactlyInAnyOrder isn't working here, and I haven't tracked down why.
    assertThat(expectedColumnRows.size).isEqualTo(actual.size)
    assertThat(expectedColumnRows.trimAndRemoveTrailingCommas().toSortedSet())
        .isEqualTo(actual.trimAndRemoveTrailingCommas().toSortedSet())
}

private fun List<String>.trimAndRemoveTrailingCommas(): List<String> {
    return map { it.removeSuffix(",").trim() }
}
