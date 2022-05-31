package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.EagerLoadDependenciesMethodNames.COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.EagerLoadDependenciesMethodNames.PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.KotlinTemplate


internal fun createEagerLoadFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className,
    ) {
        override()
        visibility { KotlinTemplate.Visibility.PROTECTED }

        writeCode { createPetalEagerLoadMethodBody(accessorClassInfo) }
    }

private fun createPetalEagerLoadMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate {
        controlFlow("return apply") {
            collectStatements {
                accessorClassInfo.petalColumns
                    .filterIsInstance<PetalReferenceColumn>()
                    .map { "${it.name}NestedPetalManager.eagerLoadAccessor()" }
            }
        }
    }

internal fun createCompanionEagerLoadDependenciesFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className,
        receiverType = accessorClassInfo.entityClassName
    ) {
        writeCode { createCompanionEagerLoadMethodBody(accessorClassInfo) }
    }

private fun createCompanionEagerLoadMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate("return load(") {
        collectCodeTemplates {
            accessorClassInfo.petalReferenceColumns
                .map { CodeTemplate("\n  %M::${it.name},", accessorClassInfo.entityMemberName) }
        }

        code { "\n).$EXPORT_METHOD_SIMPLE_NAME().eagerLoadDependencies()" }
    }

object EagerLoadDependenciesMethodNames {
    const val COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME = "toPetalWithEagerLoadedDependencies"
    const val PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME = "eagerLoadDependenciesInsideTransaction"
}
