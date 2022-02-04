package com.casadetasha.kexp.petals.processor

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.*
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.BeforeTest
import kotlin.test.Test

class MigrationCompilationTest {

    companion object {

        private val createTableSource = SourceFile.kotlin(
            "PetalSchema.kt", """
            package com.casadetasha

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import java.util.*

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 1)
            interface BasicPetalSchemaV1 {
                val uuid: UUID
                val color: String
                val count: Int
                val sporeCount: Long
            }
        """.trimIndent()
        )

        private val createAndRenameTableSource = SourceFile.kotlin(
            "PetalSchema.kt", """
            package com.casadetasha

            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import java.util.*

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 1)
            interface BasicPetalSchemaV1 {
                val uuid: UUID
                val color: String
                val count: Int
                val sporeCount: Long
            }

            @Petal(tableName = "basic_petal", className = "BasicPetal", version = 2)
            class BasicPetalSchemaV2 {
                @AlterColumn(renameFrom = "uuid") val renamed_uuid: UUID = UUID.randomUUID()
                @AlterColumn(renameFrom = "color") val renamed_color: String = ""
                @AlterColumn(renameFrom = "count") val renamed_count: Int = 1
                @AlterColumn(renameFrom = "sporeCount") val renamed_sporeCount: Long = 2
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
        """.trimIndent()
        )
    }

    private lateinit var compilationResult: KotlinCompilation.Result

    @Test
    fun `compiling sourceV1 finishes with exit code OK`() {
        compilationResult = compileSources(createTableSource)
        assertThat(compilationResult.exitCode).isEqualTo(OK)
    }

    @Test
    fun `compiling sourceV1 to sourceV2 migration finishes with exit code OK`() {
        compilationResult = compileSources(createAndRenameTableSource)
        assertThat(compilationResult.exitCode).isEqualTo(OK)
    }

    @Test
    fun `compiling migration with updated column info and no AlterColumn annotation fails`() {
        compilationResult = compileSources(unAnnotatedAlteredColumnSource)
        assertThat(compilationResult.exitCode).isEqualTo(INTERNAL_ERROR)
    }
}

internal fun compileSources(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
    return KotlinCompilation().apply {
        sources = sourceFiles.toList()
        annotationProcessors = listOf(PetalProcessor())
        inheritClassPath = true
        messageOutputStream = System.out
    }.compile()
}
