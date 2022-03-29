package com.casadetasha.kexp.petals.processor.outputgenerator

import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.model.UnprocessedPetalMigration
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.MigrationGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalMigrationSetupGenerator
import com.squareup.kotlinpoet.ClassName

internal class PetalFileGenerator(private val petalClasses: PetalClasses,
                                  private val tableMap: HashMap<String, UnprocessedPetalMigration>
) {
    fun generateFiles() {
        tableMap.values.forEach { migration ->
            generatePetalClasses(migration)
        }

        if (tableMap.isNotEmpty()) {
            PetalMigrationSetupGenerator(tableMap.values).createPetalMigrationSetupClass()
        }
    }

    private fun generatePetalClasses(migration: UnprocessedPetalMigration) {
        MigrationGenerator(migration).createMigrationForTable()
        migration.getCurrentSchema()?.let {
            val accessorClassInfo = migration.getAccessorClassInfo()

            ExposedClassesFileGenerator(petalClasses, migration.className, migration.tableName, it).generateFile()
            AccessorClassFileGenerator(accessorClassInfo).generateFile()
            DataClassFileGenerator(accessorClassInfo).generateFile()
        }
    }
}

private fun UnprocessedPetalMigration.getAccessorClassInfo(): com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo {
    return com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo(
        packageName = "com.casadetasha.kexp.petals.accessor",
        simpleName = className,
        entityClassName = ClassName("com.casadetasha.kexp.petals", "${className}Entity"),
        tableClassName = ClassName("com.casadetasha.kexp.petals", "${className}Table"),
        dataClassName = ClassName("com.casadetasha.kexp.petals.data", "${className}Data"),
        columns = getCurrentSchema()!!.columnsAsList.toSet()
    )
}
