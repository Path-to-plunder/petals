package com.casadetasha.kexp.petals.processor.outputgenerator

import com.casadetasha.kexp.petals.processor.inputparser.ParsedPetal
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.MigrationGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalMigrationSetupGenerator
import com.squareup.kotlinpoet.ClassName

internal class PetalFileGenerator(private val petalClasses: PetalClasses,
                                  private val petalMap: Map<ClassName, ParsedPetal>
) {
    fun generateFiles() {
        petalMap.values.forEach { petal ->
            generatePetalClasses(petal)
        }

        if (petalMap.isNotEmpty()) {
            PetalMigrationSetupGenerator(petalMap.values).createPetalMigrationSetupClass()
        }
    }

    private fun generatePetalClasses(petal: ParsedPetal) {
        MigrationGenerator(petal).createMigrationForTable()
        petal.getCurrentSchema()?.let {
            val accessorClassInfo = petal.getAccessorClassInfo()

            ExposedClassesFileGenerator(petalClasses, petal.petalAnnotation.className, petal.petalAnnotation.tableName, it).generateFile()
            AccessorClassFileGenerator(accessorClassInfo).generateFile()
            DataClassFileGenerator(accessorClassInfo).generateFile()
        }
    }
}

private fun ParsedPetal.getAccessorClassInfo(): com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo {
    return com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo(
        packageName = "com.casadetasha.kexp.petals.accessor",
        simpleName = petalAnnotation.className,
        entityClassName = ClassName("com.casadetasha.kexp.petals", "${className}Entity"),
        tableClassName = ClassName("com.casadetasha.kexp.petals", "${className}Table"),
        dataClassName = ClassName("com.casadetasha.kexp.petals.data", "${className}Data"),
        petalColumns = getCurrentSchema()!!.parsedPetalColumns
    )
}
