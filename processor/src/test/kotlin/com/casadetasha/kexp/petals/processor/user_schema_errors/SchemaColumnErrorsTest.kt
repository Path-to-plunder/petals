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
        assertThat(compilationResult.messages).contains("Found invalid type for column unsupportedColumn for table failed_petal schema version 1 com.casadetasha.UnsupportedColumnType is not a valid column type")
    }

    @Test
    fun `using a non basic type unsupported @ReferencedBy column type fails with internal error referencing column name and Type`() {
        val compilationResult = compileSources(basicTypeReferencedByTypeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("ReferencedBy annotated column type must be a class or interface annotated with @Petal")
        assertThat(compilationResult.messages).contains("basicTypeReferencedByColumn")
        assertThat(compilationResult.messages).contains("String")
    }

    @Test
    fun `using a basic type @ReferencedBy column type fails with internal error referencing column name and Type`() {
        val compilationResult = compileSources(unsupportedReferencedByTypeSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("ReferencedBy annotated column type must be a class or interface annotated with @Petal")
        assertThat(compilationResult.messages).contains("unsupportedReferencedByColumn")
        assertThat(compilationResult.messages).contains("NotAValidPetalFakeInterface")
    }

    @Test
    fun `using an incorrect reference column name with @ReferencedBy fails with internal error referencing column name`() {
        val compilationResult = compileSources(invalidReferencedByColumnNameSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("ReferencedBy column with name wrongColumn not found for petal type")
        assertThat(compilationResult.messages).contains("invalidReferenceByColumnName")
    }

    @Test
    fun `using a pre-existing petal class name fails with internal error referencing duplicate class name`() {
        val compilationResult = compileSources(duplicateClassNameSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("Duplicate petal class names found. All petal class names must be unique. Duplicate class names: DuplicatePetal")
    }

    @Test
    fun `using a pre-existing petal table name fails with internal error referencing duplicate table name`() {
        val compilationResult = compileSources(duplicateTableNameSchema)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
        assertThat(compilationResult.messages).contains("Duplicate petal table names found. All petal table names must be unique. Duplicate table names: duplicated_petal_table")
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

        private val basicTypeReferencedByTypeSchema = SourceFile.kotlin(
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
                @ReferencedBy("any") val basicTypeReferencedByColumn: String
            }
        """.trimIndent())

        private val unsupportedReferencedByTypeSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.ReferencedBy
            import java.util.*
            
            interface NotAValidPetalFakeInterface
            
            @Petal(tableName = "failed_petal", className = "FailedPetal")
            interface FailurePetal
            
            @PetalSchema(petal = FailurePetal::class)
            interface FailurePetalSchema {
                @ReferencedBy("any") val unsupportedReferencedByColumn: NotAValidPetalFakeInterface
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


        private val duplicateClassNameSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.ReferencedBy
            import java.util.*
            
            @Petal(tableName = "petal_table", className = "DuplicatePetal")
            interface OriginalPetal
            
            @Petal(tableName = "duplicated_petal_table", className = "DuplicatePetal")
            interface DuplicatedPetal
            
            @PetalSchema(petal = OriginalPetal::class)
            interface PetalSchema {
                val thing: String
            }
            
            @PetalSchema(petal = DuplicatedPetal::class)
            interface DuplicatedPetalSchema {
                val thing: String
            }
        """.trimIndent())

        private val duplicateTableNameSchema = SourceFile.kotlin(
            "FailureSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import com.casadetasha.kexp.petals.annotations.ReferencedBy
            import java.util.*
            
            @Petal(tableName = "duplicated_petal_table", className = "OriginalPetal")
            interface OriginalPetal
            
            @Petal(tableName = "duplicated_petal_table", className = "DuplicatePetal")
            interface DuplicatedPetal
            
            @PetalSchema(petal = OriginalPetal::class)
            interface PetalSchema {
                val thing: String
            }
            
            @PetalSchema(petal = DuplicatedPetal::class)
            interface DuplicatedPetalSchema {
                val thing: String
            }
        """.trimIndent())
    }
}