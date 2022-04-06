package com.casadetasha.kexp.petals.processor.user_schema_errors

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.processor.util.compileSources
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test

class SchemaMigrationErrorsTest {

    @Test
    fun `adding default value without AlterColumn annotation fails referencing column name`() {
        val compilationResult = compileSources(unannotatedAddDefaultSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("defaultValueChangeColumn")
    }

    @Test
    fun `removing default value without AlterColumn annotation fails referencing column name`() {
        val compilationResult = compileSources(unannotatedRemoveDefaultSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("defaultValueChangeColumn")
    }

    @Test
    fun `changing to nullable without AlterColumn annotation fails referencing column name`() {
        val compilationResult = compileSources(unannotatedMakeNullableChangeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("nullChangeColumn")
    }

    @Test
    fun `changing to non-nullable without AlterColumn annotation fails referencing column name`() {
        val compilationResult = compileSources(unannotatedMakeNonNullableChangeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("nullChangeColumn")
    }

    @Test
    fun `changing type of column fails with internal error referencing column name`() {
        val compilationResult = compileSources(columnTypeChangeChangeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("typeChangeColumn")
    }

    @Test
    fun `alterColumn name change with no changes fails with internal error referencing column name`() {
        val compilationResult = compileSources(alterColumnNameWithoutChangesSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("alterWithoutChanging")
    }

    @Test
    fun `alterColumn name change with no matching previous column referencing column name`() {
        val compilationResult = compileSources(alterColumnNameWithoutPreviousColumnSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("newColumn")
        assertThat(compilationResult.messages).contains("wrongColumn")
    }

    companion object {

        private val unannotatedAddDefaultSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.DefaultString
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface BasicPetal
            
            @PetalSchema(petal = BasicPetal::class, version = 1)
            interface BasicPetalSchemaV1 {
                val defaultValueChangeColumn: String
            }
            
            @PetalSchema(petal = BasicPetal::class, version = 2)
            interface BasicPetalSchemaV2 {
                @DefaultString("This not gonna work") val defaultValueChangeColumn: String
            }
        """.trimIndent())

        private val unannotatedRemoveDefaultSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.DefaultString
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface BasicPetal
            
            @PetalSchema(petal = BasicPetal::class, version = 1)
            interface BasicPetalSchemaV1 {
                @DefaultString("This not gonna work") val defaultValueChangeColumn: String
            }
            
            @PetalSchema(petal = BasicPetal::class, version = 2)
            interface BasicPetalSchemaV2 {
                val defaultValueChangeColumn: String
            }
        """.trimIndent())

        private val unannotatedMakeNullableChangeSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface BasicPetal
            
            @PetalSchema(petal = BasicPetal::class, version = 1)
            interface BasicPetalSchemaV1 {
                val nullChangeColumn: UUID
            }
            
            @PetalSchema(petal = BasicPetal::class, version = 2)
            interface BasicPetalSchemaV2 {
                val nullChangeColumn: UUID?
            }
        """.trimIndent())

        private val unannotatedMakeNonNullableChangeSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface BasicPetal
            
            @PetalSchema(petal = BasicPetal::class, version = 1)
            interface BasicPetalSchemaV1 {
                val nullChangeColumn: UUID?
            }
            
            @PetalSchema(petal = BasicPetal::class, version = 2)
            interface BasicPetalSchemaV2 {
                val nullChangeColumn: UUID
            }
        """.trimIndent())

        private val columnTypeChangeChangeSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = FailurePetal::class, version = 1)
            interface FailurePetalSchemaV1 {
                val typeChangeColumn: UUID
            }
            
            @PetalSchema(petal = FailurePetal::class, version = 2)
            interface FailurePetalSchemaV2 {
                @AlterColumn val typeChangeColumn: String
            }
        """.trimIndent())

        private val alterColumnNameWithoutChangesSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = FailurePetal::class, version = 1)
            interface FailurePetalSchemaV1 {
                val alterWithoutChanging: UUID
            }
            
            @PetalSchema(petal = FailurePetal::class, version = 2)
            interface FailurePetalSchemaV2 {
                @AlterColumn("alterWithoutChanging") val alterWithoutChanging: UUID
            }
        """.trimIndent())

        private val alterColumnNameWithoutPreviousColumnSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = FailurePetal::class, version = 1)
            interface FailurePetalSchemaV1 {
                val oldColumn: UUID
            }
            
            @PetalSchema(petal = FailurePetal::class, version = 2)
            interface FailurePetalSchemaV2 {
                @AlterColumn("wrongColumn") val newColumn: UUID
            }
        """.trimIndent())
    }
}