package com.casadetasha.kexp.petals.processor.outputgenerator

import com.casadetasha.kexp.petals.processor.model.ParsedPetal
import com.casadetasha.kexp.petals.processor.model.PetalClasses
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DslDataClassFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.exposed.ExposedClassesFileGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.MigrationGenerator
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.migration.PetalMigrationSetupGenerator
import com.squareup.kotlinpoet.ClassName

internal class PetalFileGenerator(
    private val petalClasses: PetalClasses
) {
    private val petalMap by lazy { petalClasses.petalMap }

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
        petal.currentSchema?.let {
            val accessorClassInfo = petal.getAccessorClassInfo()

            ExposedClassesFileGenerator(petalClasses, petal.petalAnnotation.className, petal.petalAnnotation.tableName, it).generateFile()
            AccessorClassFileGenerator(accessorClassInfo).generateFile()
            DslDataClassFileGenerator.generateFile(accessorClassInfo)
        }
    }
}

private fun ParsedPetal.getAccessorClassInfo(): AccessorClassInfo {
    return AccessorClassInfo(
        packageName = "com.casadetasha.kexp.petals.accessor",
        simpleName = baseSimpleName,
        entityClassName = ClassName("com.casadetasha.kexp.petals", "${baseSimpleName}Entity"),
        tableClassName = ClassName("com.casadetasha.kexp.petals", "${baseSimpleName}Table"),
        dataClassName = ClassName("com.casadetasha.kexp.petals.data", "${baseSimpleName}Data"),
        petalColumns = currentSchema!!.parsedPetalColumns
    )
}
