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

class StartingNonDefaultValueSqlTest {

    @Test
    fun `Creates column without default value if no default value is set`() {
        val migrationSql = petalSchemaMigrations[1]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).createsTableWithExactColumns(
            tableName = TABLE_NAME,
            columnCreationSql = listOf(
                    " id uuid PRIMARY KEY,",
                    " \"color\" TEXT"
            )
        )
    }

    @Test
    fun `Updates column to NOT NULL if altered column adds default value`() {
        val migrationSql = petalSchemaMigrations[2]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql)
            .migratesTableWithExactColumnAlterations(
                tableName = TABLE_NAME,
                columnAlterationSql = listOf(" ALTER COLUMN \"color\" SET DEFAULT 'Yellow'")
            )
    }

    companion object {

        private const val TABLE_NAME = "starting_non_default_value_petal"

        private val createAndDropDefaultValueSource = SourceFile.kotlin(
            "StartingNullablePetalSchemas.kt", """
            package com.casadetasha.kexp.petals.processor.post
            
            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.DefaultString
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "$TABLE_NAME", className = "StartingNonDefaultValuePetal")
            interface StartingNonDefaultValuePetal
            
            @PetalSchema(petal = StartingNonDefaultValuePetal::class, version = 1)
            interface StartingNonDefaultPetalSchemaV1 {
                val color: String?
            }
            
            @PetalSchema(petal = StartingNonDefaultValuePetal::class, version = 2)
            interface StartingNonDefaultValuePetalSchemaV2 {
                @AlterColumn @DefaultString("Yellow") val color: String?
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
                val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$$TABLE_NAME")

                val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
                val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
                val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
                val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

                petalSchemaMigrations = petalMigration.schemaMigrations
            }
        }
    }
}