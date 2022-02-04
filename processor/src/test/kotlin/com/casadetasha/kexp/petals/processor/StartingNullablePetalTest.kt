package com.casadetasha.kexp.petals.processor

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.junit.ClassRule
import org.junit.Test
import org.junit.rules.ExternalResource
import java.lang.reflect.Method

class StartingNullablePetalTest {

    companion object {

        private val createAndRenameTableSource = SourceFile.kotlin(
            "StartingNullablePetalSchemas.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import java.util.*

            @Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal", version = 1)
            interface StartingNullablePetalSchemaV1 {
                val color: String?
            }
            
            @Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal", version = 2)
            interface StartingNullablePetalSchemaV2 {
                @AlterColumn val color: String
            }
            
            @Petal(tableName = "starting_nullable_petal", className = "StartingNullablePetal", version = 3)
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

    @Test
    fun `Creates column as nullable if schema property is nullable`() {
        assertThat(petalSchemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE \"starting_nullable_petal\" (" +
                    " \"color\" TEXT" +
                    " )"
            )
    }

    @Test
    fun `Updates column to nullable if altered column is nullable`() {
        assertThat(petalSchemaMigrations[2]!!.migrationSql)
            .isEqualTo("ALTER TABLE \"starting_nullable_petal\"" +
                    " ALTER COLUMN \"color\" SET NOT NULL")
    }

    @Test
    fun `Added nullable columns are added as nullable`() {
        assertThat(petalSchemaMigrations[3]!!.migrationSql)
            .isEqualTo("ALTER TABLE \"starting_nullable_petal\"" +
                    " ADD COLUMN \"secondColor\" TEXT"
            )
    }
}