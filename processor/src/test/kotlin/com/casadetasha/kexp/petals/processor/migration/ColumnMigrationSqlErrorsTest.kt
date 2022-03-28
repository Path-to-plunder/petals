package com.casadetasha.kexp.petals.processor.migration

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.processor.util.compileSources
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.junit.Test

class ColumnMigrationSqlErrorsTest {

    @Test
    fun `compiling migration with updated column info and no AlterColumn annotation fails`() {
        val compilationResult = compileSources(unAnnotatedAlteredColumnSource)
        assertThat(compilationResult.exitCode).isEqualTo(KotlinCompilation.ExitCode.INTERNAL_ERROR)
    }

    companion object {

        private val unAnnotatedAlteredColumnSource = SourceFile.kotlin(
            "PetalSchema.kt", """
            package com.casadetasha
            
            import com.casadetasha.kexp.petals.annotations.AlterColumn
            import com.casadetasha.kexp.petals.annotations.Petal
            import com.casadetasha.kexp.petals.annotations.PetalSchema
            import java.util.*
            
            @Petal(tableName = "basic_petal", className = "BasicPetal")
            interface BasicPetal
            
            @PetalSchema(petal = BasicPetal::class, version = 1)
            interface BasicPetalSchemaV1 {
                val uuid: UUID
            }
            
            @PetalSchema(petal = BasicPetal::class, version = 2)
            interface BasicPetalSchemaV2 {
                val uuid: UUID?
            }
        """.trimIndent())
    }
}