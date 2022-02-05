package com.casadetasha.kexp.petals.processor

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
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

class ColumnMigrationSqlTest {

    companion object {

        private val createAndRenameTableSource = SourceFile.kotlin(
            "PetalSchema.kt", """
            package com.casadetasha.kexp.petals.processor.post

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalPrimaryKey
            import com.casadetasha.kexp.petals.annotations.VarChar
            import java.util.*
            
            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 1, primaryKeyType = PetalPrimaryKey.INT)
            interface BasicPetalSchemaV1 {
                val checkingInt: Int
                val checkingLong: Long
                val checkingString: String
                @VarChar(charLimit = 10) val checkingVarChar: String
                val checkingUUID: UUID
            }

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 2, primaryKeyType = PetalPrimaryKey.INT)
            interface BasicPetalSchemaV2 {
                val uuid: UUID
                val color: String
                @VarChar(charLimit = 10) val secondColor: String
                val count: Int
                val sporeCount: Long
            }

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 3, primaryKeyType = PetalPrimaryKey.INT)
            abstract class BasicPetalSchemaV3 {
                @AlterColumn(renameFrom = "uuid") abstract val renamed_uuid: UUID
                @AlterColumn(renameFrom = "color") abstract val renamed_color: String
                @AlterColumn(renameFrom = "secondColor") @VarChar(charLimit = 10) abstract val renamed_secondColor: String
                @AlterColumn(renameFrom = "count") abstract val renamed_count: Int
                @AlterColumn(renameFrom = "sporeCount") abstract val renamed_sporeCount: Long
            }
            """.trimIndent()
        )

        private val unAnnotatedAlteredColumnSource = SourceFile.kotlin(
            "PetalSchema.kt", """
            package com.casadetasha

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import java.util.*

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 1)
            interface BasicPetalSchemaV1 {
                val uuid: UUID
            }

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 2)
            interface BasicPetalSchemaV2 {
                val uuid: UUID?
            }
        """.trimIndent())

        private lateinit var petalSchemaMigrations: Map<Int, PetalSchemaMigration>

        @ClassRule
        @JvmField
        val resource: ExternalResource = object : ExternalResource() {

            override fun before() {
                val compilationResult = compileSources(createAndRenameTableSource)
                check(compilationResult.exitCode == KotlinCompilation.ExitCode.OK)
                parsePetalMigrations(compilationResult)
            }

            private fun parsePetalMigrations(compilationResult: KotlinCompilation.Result) {
                val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$basic_petal")

                val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
                val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
                val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
                val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

                petalSchemaMigrations = petalMigration.schemaMigrations
            }
        }
    }

    @Test
    fun `Creates table creation migration with all supported types`() {
        assertThat(petalSchemaMigrations[1]!!.migrationSql)
            .isEqualTo(
                "CREATE TABLE \"basic_petal\" (" +
                        " id SERIAL PRIMARY KEY," +
                        " \"checkingVarChar\" CHARACTER VARYING(10) NOT NULL," +
                        " \"checkingString\" TEXT NOT NULL," +
                        " \"checkingInt\" INT NOT NULL," +
                        " \"checkingUUID\" uuid NOT NULL," +
                        " \"checkingLong\" BIGINT NOT NULL" +
                        " )"
            )
    }

    @Test
    fun `Creates alter table migration with dropping and adding all supported types`() {
        assertThat(petalSchemaMigrations[2]!!.migrationSql)
            .isEqualTo(
                "ALTER TABLE \"basic_petal\"" +
                        " DROP COLUMN \"checkingVarChar\"," +
                        " DROP COLUMN \"checkingString\"," +
                        " DROP COLUMN \"checkingInt\"," +
                        " DROP COLUMN \"checkingUUID\"," +
                        " DROP COLUMN \"checkingLong\"," +
                        " ADD COLUMN \"color\" TEXT NOT NULL," +
                        " ADD COLUMN \"count\" INT NOT NULL," +
                        " ADD COLUMN \"secondColor\" CHARACTER VARYING(10) NOT NULL," +
                        " ADD COLUMN \"uuid\" uuid NOT NULL," +
                        " ADD COLUMN \"sporeCount\" BIGINT NOT NULL"
            )
    }

    @Test
    fun `Does not create migrationSql if there are only rename alterations`() {
        val renameMigration = petalSchemaMigrations[3]!!
        val migrationSql: String? = renameMigration.migrationSql

        assertThat(migrationSql).isNull()
    }

    @Test
    fun `Creates alterationSql migrations to rename all supported types`() {
        val renameMigration = petalSchemaMigrations[3]!!
        val alterationSql: List<String>? = renameMigration.migrationAlterationSql

        assertThat(alterationSql).isNotNull()
        assertThat(alterationSql!!).containsExactly(
            """ALTER TABLE "basic_petal" RENAME COLUMN "count" TO "renamed_count";""",
            """ALTER TABLE "basic_petal" RENAME COLUMN "sporeCount" TO "renamed_sporeCount";""",
            """ALTER TABLE "basic_petal" RENAME COLUMN "uuid" TO "renamed_uuid";""",
            """ALTER TABLE "basic_petal" RENAME COLUMN "secondColor" TO "renamed_secondColor";""",
            """ALTER TABLE "basic_petal" RENAME COLUMN "color" TO "renamed_color";"""
        )
    }

    @Test
    fun `compiling migration with updated column info and no AlterColumn annotation fails`() {
        val compilationResult = compileSources(unAnnotatedAlteredColumnSource)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
    }
}