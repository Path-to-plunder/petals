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
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.lang.reflect.Method

@OptIn(ExperimentalCompilerApi::class)
class StartingNullablePetalTest {

    @Test
    fun `Creates column as nullable if schema property is nullable`() {
        val migrationSql = petalSchemaMigrations[1]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).createsTableWithExactColumns(
                tableName = "starting_nullable_petal",
                columnCreationSql = listOf(
                    " id uuid PRIMARY KEY,",
                    " \"color\" TEXT"
                )
            )
    }

    @Test
    fun `Updates column to nullable if altered column is nullable`() {
        val migrationSql = petalSchemaMigrations[2]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).migratesTableWithExactColumnAlterations(
            tableName = "starting_nullable_petal",
            columnAlterationSql = listOf(" ALTER COLUMN \"color\" SET NOT NULL")
        )
    }

    @Test
    fun `Added nullable columns are added as nullable`() {
        val migrationSql = petalSchemaMigrations[3]!!.migrationSqlRows

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThatSqlList(migrationSql).migratesTableWithExactColumnAlterations(
            tableName = "starting_nullable_petal",
            columnAlterationSql = listOf(" ADD COLUMN \"secondColor\" TEXT")
        )
    }

    companion object {

        private val createAndRenameTableSource = SourceFile.kotlin(
            "StartingNullablePetalSchemas.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*

            @Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal")
            interface StartingNullablePetal

            @PetalSchema(petal = StartingNullablePetal::class, version = 1)
            interface StartingNullablePetalSchemaV1 {
                val color: String?
            }
            
            @PetalSchema(petal = StartingNullablePetal::class, version = 2)
            interface StartingNullablePetalSchemaV2 {
                @AlterColumn val color: String
            }
            
            @PetalSchema(petal = StartingNullablePetal::class, version = 3)
            interface StartingNullablePetalSchemaV3 {
                val color: String
                val secondColor: String?
            }
            """.trimIndent()
        )

        private lateinit var petalSchemaMigrations: Map<Int, PetalSchemaMigration>

        @ClassRule
        @JvmField
        val resource: ExternalResource = object : ExternalResource() {

            override fun before() {
                val compilationResult = compileSources(createAndRenameTableSource)
                check(compilationResult.exitCode == KotlinCompilation.ExitCode.OK) {
                    compilationResult.messages
                }
                parsePetalMigrations(compilationResult)
            }

            private fun parsePetalMigrations(compilationResult: KotlinCompilation.Result) {
                val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$starting_nullable_petal")

                val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
                val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
                val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
                val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

                petalSchemaMigrations = petalMigration.schemaMigrations
            }
        }
    }
}