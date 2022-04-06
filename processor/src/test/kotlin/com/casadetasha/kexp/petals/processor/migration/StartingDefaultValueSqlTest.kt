package com.casadetasha.kexp.petals.processor.migration

import assertk.assertThat
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.processor.util.compileSources
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.lang.reflect.Method

class StartingDefaultValueSqlTest {

    @Test
    fun `Creates column with default value if schema property has default`() {
        val migrationSql = petalSchemaMigrations[1]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).createsTableWithExactColumns(
            tableName = "starting_default_value_petal",
            columnCreationSql = listOf(
                    " id uuid PRIMARY KEY,",
                    " \"color\" TEXT DEFAULT 'Blue'"
            )
        )
    }

    @Test
    fun `Updates column to NOT NULL if altered column does not have default`() {
        val migrationSql = petalSchemaMigrations[2]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql)
            .migratesTableWithExactColumnAlterations(
                tableName = "starting_default_value_petal",
                columnAlterationSql = listOf(" ALTER COLUMN \"color\" DROP DEFAULT")
            )
    }

    companion object {

        private val createAndDropDefaultValueSource = SourceFile.kotlin(
            "StartingNullablePetalSchemas.kt", """
            package com.casadetasha.kexp.petals.processor.post
            
            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.DefaultString
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "starting_default_value_petal", className = "StartingDefaultValuePetal")
            interface StartingDefaultValuePetal
            
            @PetalSchema(petal = StartingDefaultValuePetal::class, version = 1)
            interface StartingDefaultPetalSchemaV1 {
                @DefaultString("Blue") val color: String?
            }
            
            @PetalSchema(petal = StartingDefaultValuePetal::class, version = 2)
            interface StartingDefaultValuePetalSchemaV2 {
                @AlterColumn val color: String?
            }
            """.trimIndent()
        )

        private lateinit var petalSchemaMigrations: Map<Int, PetalSchemaMigration>

        @ClassRule
        @JvmField
        val resource: ExternalResource = object : ExternalResource() {

            override fun before() {
                val compilationResult = compileSources(createAndDropDefaultValueSource)
                check(compilationResult.exitCode == KotlinCompilation.ExitCode.OK) {
                    compilationResult.messages
                }
                parsePetalMigrations(compilationResult)
            }

            private fun parsePetalMigrations(compilationResult: KotlinCompilation.Result) {
                val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$starting_default_value_petal")

                val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
                val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
                val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
                val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

                petalSchemaMigrations = petalMigration.schemaMigrations
            }
        }
    }
}