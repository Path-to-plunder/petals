package com.casadetasha.kexp.petals.processor.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
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
class RunBeforeMigrationSqlTest {

    @Test
    fun `Adds RunBeforeMigration value to table creation PetalSchemaMigration`() {
        val migrationSql = petalSchemaMigrations[1]!!.preMigrationSql

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThat(migrationSql).isEqualTo("RUN BEFORE CREATING TABLE")
    }

    @Test
    fun `Adds RunBeforeMigration value to alter table PetalSchemaMigration`() {
        val migrationSql = petalSchemaMigrations[2]!!.preMigrationSql

        assertThat(migrationSql).isNotNull()
        assertThat(migrationSql!!).isNotEmpty()
        assertThat(migrationSql).isEqualTo("RUN BEFORE ALTERING TABLE")
    }

    companion object {
        private const val TABLE_NAME = "run_before_migration_table"

        private val createAndDropDefaultValueSource = SourceFile.kotlin(
            "StartingNullablePetalSchemas.kt", """
            package com.casadetasha.kexp.petals.processor.post
            
            import com.casadetasha.kexp.petals.annotations.*
            import java.util.*
            
            @Petal(tableName = "$TABLE_NAME", className = "StartingDefaultValuePetal")
            interface RunBeforeMigrationPetal
            
            @PetalSchema(petal = RunBeforeMigrationPetal::class, version = 1)
            @ExecuteSqlBeforeMigration("RUN BEFORE CREATING TABLE")
            interface RunBeforeMigrationPetalSchemaV1 {
                val color: String
            }
            
            @PetalSchema(petal = RunBeforeMigrationPetal::class, version = 2)
            @ExecuteSqlBeforeMigration("RUN BEFORE ALTERING TABLE")
            interface RunBeforeMigrationPetalSchemaV2 {
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