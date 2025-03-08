package com.casadetasha.kexp.petals.processor.speed

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.casadetasha.kexp.petals.annotations.PetalMigration
import com.casadetasha.kexp.petals.annotations.PetalSchemaMigration
import com.casadetasha.kexp.petals.processor.util.compileSources
import com.casadetasha.kexp.petals.processor.util.countMilliseconds
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Ignore
import org.junit.Test
import java.lang.reflect.Method

@Ignore
@OptIn(ExperimentalCompilerApi::class)
class PetalCompileSpeedCheck {

    @Test
    fun `time test for 2 columns and 50 tables`() {
        testSmallTables(tableCount = 50)
    }

    @Test
    fun `time test for 2 columns and 100 tables`() {
        testSmallTables(tableCount = 100)
    }

    @Test
    fun `time test for 2 columns and 150 tables`() {
        testSmallTables(tableCount = 150)
    }

    @Test
    fun `time test for 10 columns and 50 tables`() {
        testTables(tableCount = 50)
    }

    @Test
    fun `time test for 10 columns and 150 tables`() {
        testTables(tableCount = 150)
    }

    @Test
    fun `time test for 10 columns with a migration changing every column 50 tables`() {
        testMigratedTables(tableCount = 50)
    }

    @Test
    fun `time test for 10 columns with a migration changing every column 100 tables`() {
        testMigratedTables(tableCount = 100)
    }

    @Test
    fun `time test for 10 columns with a migration changing every column 150 tables`() {
        testMigratedTables(tableCount = 150)
    }

    private fun testSmallTables(tableCount: Int) {
        var testSchemaFile = TEST_TABLES_HEADER
        (1..tableCount).forEach {
            testSchemaFile += createTestSmallTable(it)
        }

        var compilationResult: KotlinCompilation.Result? = null
        val compileTime = countMilliseconds {
            compilationResult = compileSources(SourceFile.kotlin("TestTableSchemas.kt", testSchemaFile))
        }


        check(compilationResult!!.exitCode == KotlinCompilation.ExitCode.OK) {
            compilationResult!!.messages
        }

        val migrations = parsePetalMigrations(compilationResult!!, "test_small_table_1")

        assertThat(migrations.size).isEqualTo(1)
        assertThat(compileTime).isEqualTo(0L)
    }

    private fun testTables(tableCount: Int) {
        var testSchemaFile = TEST_TABLES_HEADER
        (1..tableCount).forEach {
            testSchemaFile += createTestTable(it)
        }

        var compilationResult: KotlinCompilation.Result? = null
        val compileTime = countMilliseconds {
            compilationResult = compileSources(SourceFile.kotlin("TestTableSchemas.kt", testSchemaFile))
        }


        check(compilationResult!!.exitCode == KotlinCompilation.ExitCode.OK) {
            compilationResult!!.messages
        }

        val migrations = parsePetalMigrations(compilationResult!!, "test_table_1")

        assertThat(migrations.size).isEqualTo(1)
        assertThat(compileTime).isEqualTo(0L)
    }

    private fun testMigratedTables(tableCount: Int) {
        var testSchemaFile = TEST_TABLES_HEADER
        (1..tableCount).forEach {
            testSchemaFile += createTestMigratedTable(it)
        }

        var compilationResult: KotlinCompilation.Result? = null
        val compileTime = countMilliseconds {
            compilationResult = compileSources(SourceFile.kotlin("TestMigratedTableSchemas.kt", testSchemaFile))
        }


        check(compilationResult!!.exitCode == KotlinCompilation.ExitCode.OK) {
            compilationResult!!.messages
        }

        val migrations = parsePetalMigrations(compilationResult!!, "test_migrated_table_1")

        assertThat(migrations.size).isEqualTo(2)
        assertThat(compileTime).isEqualTo(0L)
    }

    private fun parsePetalMigrations(compilationResult: KotlinCompilation.Result, tableName: String): Map<Int, PetalSchemaMigration> {
        val generatedMigrationClass = compilationResult.classLoader.loadClass("com.casadetasha.kexp.petals.migration.TableMigrations\$$tableName")

        val migrationClassInstance = generatedMigrationClass.getDeclaredConstructor().newInstance()
        val petalJsonGetter: Method = migrationClassInstance.javaClass.getDeclaredMethod("getPetalJson")
        val petalJson: String = petalJsonGetter.invoke(migrationClassInstance) as String
        val petalMigration: PetalMigration = Json.decodeFromString(petalJson)

        return petalMigration.schemaMigrations
    }

    companion object {

        private const val TEST_TABLES_HEADER = "" +
                "package com.casadetasha.kexp.petals.processor.post\n" +
                "\n" +
                "import com.casadetasha.kexp.petals.annotations.Petal\n" +
                "import com.casadetasha.kexp.petals.annotations.PetalSchema\n" +
                "import com.casadetasha.kexp.petals.annotations.AlterColumn\n" +
                "\n"

        private fun createTestSmallTable(number: Int) = "" +
                "@Petal(tableName = \"test_small_table_$number\", className = \"TestSmallTable$number\")\n" +
                "interface TestSmallTable$number\n" +
                "\n" +
                "@PetalSchema(petal = TestSmallTable$number::class, version = 1)\n" +
                "interface TestSmallTable${number}Schema {\n" +
                "  val firstColumn: String\n" +
                "  val secondColumn: String?\n" +
                "}\n" +
                "\n"

        private fun createTestTable(number: Int) = "" +
                "@Petal(tableName = \"test_table_$number\", className = \"TestTable$number\")\n" +
                "interface TestTable$number\n" +
                "\n" +
                "@PetalSchema(petal = TestTable$number::class, version = 1)\n" +
                "interface TestTable${number}Schema {\n" +
                "  val firstColumn: String\n" +
                "  val secondColumn: String?\n" +
                "  val thirdColumn: String?\n" +
                "  val fourthColumn: String?\n" +
                "  val fifthColumn: String?\n" +
                "  val sixthColumn: String?\n" +
                "  val seventhColumn: String?\n" +
                "  val eighthColumn: String?\n" +
                "  val ninthColumn: String?\n" +
                "  val tenthColumn: String?\n" +
                "}\n" +
                "\n"

        private fun createTestMigratedTable(number: Int) = "" +
                "@Petal(tableName = \"test_migrated_table_$number\", className = \"TestTable$number\")\n" +
                "interface TestMigratedTable$number\n" +
                "\n" +
                "@PetalSchema(petal = TestMigratedTable$number::class, version = 1)\n" +
                "interface TestMigratedTable${number}Schema {\n" +
                "  val firstColumn: String\n" +
                "  val secondColumn: String?\n" +
                "  val thirdColumn: String?\n" +
                "  val fourthColumn: String?\n" +
                "  val fifthColumn: String?\n" +
                "  val sixthColumn: String?\n" +
                "  val seventhColumn: String?\n" +
                "  val eighthColumn: String?\n" +
                "  val ninthColumn: String?\n" +
                "  val tenthColumn: String?\n" +
                "}\n" +
                "\n" +
                "@PetalSchema(petal = TestMigratedTable$number::class, version = 2)\n" +
                "interface TestMigratedTable${number}SchemaV2 {\n" +
                "  @AlterColumn val firstColumn: String?\n" +
                "  @AlterColumn val secondColumn: String\n" +
                "  @AlterColumn val thirdColumn: String\n" +
                "  @AlterColumn val fourthColumn: String\n" +
                "  @AlterColumn val fifthColumn: String\n" +
                "  @AlterColumn val sixthColumn: String\n" +
                "  @AlterColumn val seventhColumn: String\n" +
                "  @AlterColumn val eighthColumn: String\n" +
                "  @AlterColumn val ninthColumn: String\n" +
                "  @AlterColumn val tenthColumn: String\n" +
                "}\n" +
                "\n"
    }
}
