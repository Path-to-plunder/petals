package com.casadetasha.kexp.petals.processor

import assertk.assertThat
import assertk.assertions.isEqualTo
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

class StartingNonNullableSqlTest {

    companion object {

        private val createAndRenameTableSource = SourceFile.kotlin(
            "StartingNullablePetalSchemas.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import java.util.*


            @Petal(tableName = "starting_non_nullable_petal", className = "StartingNonNullablePetal", version = 1)
            interface StartingNonNullablePetalSchemaV1 {
                val color: String
            }

            @Petal(tableName = "starting_non_nullable_petal", className = "StartingNonNullablePetal", version = 2)
            interface StartingNonNullablePetalSchemaV2 {
                @AlterColumn val color: String?
            }

            @Petal(tableName = "starting_non_nullable_petal", className = "StartingNonNullablePetal", version = 3)
            interface StartingNonNullablePetalSchemaV3 {
                val color: String?
                val secondColor: String
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
                val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$starting_non_nullable_petal")

                val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
                val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
                val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
                val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

                petalSchemaMigrations = petalMigration.schemaMigrations
            }
        }
    }

    @Test
    fun `Creates column as NOT NULL if schema property is not nullable`() {
        assertThat(petalSchemaMigrations[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE \"starting_non_nullable_petal\" (" +
                    " id SERIAL PRIMARY KEY," +
                    " \"color\" TEXT NOT NULL" +
                    " )"
            )
    }

    @Test
    fun `Updates column to NOT NULL if altered column is non nullable`() {
        assertThat(petalSchemaMigrations[2]!!.migrationSql)
            .isEqualTo("ALTER TABLE \"starting_non_nullable_petal\"" +
                    " ALTER COLUMN \"color\" DROP NOT NULL"
            )
    }

    @Test
    fun `Added non nullable columns are added as non nullable`() {
        assertThat(petalSchemaMigrations[3]!!.migrationSql)
            .isEqualTo("ALTER TABLE \"starting_non_nullable_petal\"" +
                    " ADD COLUMN \"secondColor\" TEXT NOT NULL"
            )
    }
}