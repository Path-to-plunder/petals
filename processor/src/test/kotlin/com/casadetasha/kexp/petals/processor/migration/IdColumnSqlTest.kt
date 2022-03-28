package com.casadetasha.kexp.petals.processor.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
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
        assertThat(petalSchemaMigration[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE \"int_id_petal\" (" +
                    " id SERIAL PRIMARY KEY," +
                    " \"column\" TEXT NOT NULL" +
                    " )"
            )
    }
    @Test
    fun `Creates table with long id`() {
        val petalSchemaMigration = generateSchemaMigrations(longIdPetalSchema, "long_id_petal")
        assertThat(petalSchemaMigration[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE \"long_id_petal\" (" +
                    " id BIGSERIAL PRIMARY KEY," +
                    " \"column\" TEXT NOT NULL" +
                    " )"
            )
    }


    @Test
    fun `Creates table with uuid id`() {
        val petalSchemaMigration = generateSchemaMigrations(uuidIdPetalSchema, "uuid_id_petal")
        assertThat(petalSchemaMigration[1]!!.migrationSql)
            .isEqualTo("CREATE TABLE \"uuid_id_petal\" (" +
                    " id uuid PRIMARY KEY," +
                    " \"column\" TEXT NOT NULL" +
                    " )"
            )
    }

    private fun generateSchemaMigrations(sourceFile: SourceFile, tableName: String): Map<Int, PetalSchemaMigration> {
        val compilationResult = compileSources(sourceFile)
        val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$$tableName")

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