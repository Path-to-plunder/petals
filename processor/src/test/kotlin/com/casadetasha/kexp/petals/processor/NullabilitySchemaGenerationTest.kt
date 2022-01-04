package com.casadetasha.kexp.petals.processor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.OK
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.Test

class NullabilitySchemaGenerationTest {
    companion object {
        private val createTableSource = SourceFile.kotlin(
            "PetalSchema.kt", """
            package com.casadetasha

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal

            @Petal(tableName = "starting_nullable_petal", version = 1)
            interface StartingNullablePetalSchema {
                val color: String?
            }

            @Petal(tableName = "starting_non_nullable_petal", version = 1)
            interface StartingNonNullablePetalSchemaV1 {
                val color: String
            }

            @Petal(tableName = "starting_non_nullable_petal", version = 2)
            interface StartingNonNullablePetalSchemaV2 {
                @AlterColumn(renameFrom = "color") val color: String?
            }
        """.trimIndent()
        )
    }

    private lateinit var compilationResult: KotlinCompilation.Result

    @Test
    fun `compiling sourceV1 finishes with exit code OK`() {
        compilationResult = compileSources(createTableSource)
        assertThat(compilationResult.exitCode).isEqualTo(OK)
    }

    private fun compileSources(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
        return KotlinCompilation().apply {
            sources = sourceFiles.toList()
            annotationProcessors = listOf(PetalProcessor())
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()
    }
}
