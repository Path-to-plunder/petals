package com.casadetasha.kexp.petals.processor.user_schema_errors

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.processor.util.compileSources
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test

class SchemaColumnErrorsTest {

    @Test
    fun `using an unsupported column type fails with internal error referencing column name and Type`() {
        val compilationResult = compileSources(unsupportedColumnTypeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("Column type must be a base Petal column type or another Petal")
        assertThat(compilationResult.messages).contains("unsupportedColumn")
        assertThat(compilationResult.messages).contains("UnsupportedColumnType")
    }

    @Test
    fun `using an unsupported @ReferencedBy column type fails with internal error referencing column name and Type`() {
        val compilationResult = compileSources(unsupportedReferencedByTypeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("ReferencedBy type must be a base Petal column type or another Petal.")
        assertThat(compilationResult.messages).contains("unsupportedReferencedByColumn")
        assertThat(compilationResult.messages).contains("String")
    }

    @Test
    fun `using an incorrect reference column name with @ReferencedBy fails with internal error referencing column name`() {
        val compilationResult = compileSources(invalidReferencedByColumnNameSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("ReferencedBy column with name wrongColumn not found for petal type")
        assertThat(compilationResult.messages).contains("invalidReferenceByColumnName")
    }

    companion object {

        private val unsupportedColumnTypeSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            interface UnsupportedColumnType
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = FailurePetal::class)
            interface FailurePetalSchema {
                val unsupportedColumn: UnsupportedColumnType
            }
        """.trimIndent())

        private val unsupportedReferencedByTypeSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.ReferencedBy
            import java.util.*
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = FailurePetal::class)
            interface FailurePetalSchema {
                @ReferencedBy("any") val unsupportedReferencedByColumn: String
            }
        """.trimIndent())

        private val invalidReferencedByColumnNameSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.ReferencedBy
            import java.util.*
            
            @Petal(tableName = "referencing_failed_petal", className = "ReferencingFailedPetal")
            interface ReferencingFailurePetal
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = ReferencingFailurePetal::class)
            interface ReferencingFailurePetalSchema {
                val correctColumn: FailurePetal
            }
            
            @PetalSchema(petal = FailurePetal::class)
            interface FailurePetalSchema {
                @ReferencedBy("wrongColumn") val invalidReferenceByColumnName: ReferencingFailurePetal
            }
        """.trimIndent())
    }
}