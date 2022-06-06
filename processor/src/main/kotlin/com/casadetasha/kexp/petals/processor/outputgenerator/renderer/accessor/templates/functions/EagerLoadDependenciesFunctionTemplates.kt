package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.EagerLoadDependenciesMethodNames.COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.EagerLoadDependenciesMethodNames.PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.KotlinTemplate

internal fun createEagerLoadFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = PETAL_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className,
    ) {
        override()
        visibility { KotlinTemplate.Visibility.PROTECTED }

        methodBody(createPetalEagerLoadMethodBody(accessorClassInfo))
    }

private fun createPetalEagerLoadMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate {
        controlFlowCode("return apply", endFlowString = "}") {
            collectCodeLines {
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
        methodBody(createCompanionEagerLoadMethodBody(accessorClassInfo))
    }

private fun createCompanionEagerLoadMethodBody(accessorClassInfo: AccessorClassInfo): CodeTemplate =
    CodeTemplate("return load(") {
        collectCodeTemplates {
            accessorClassInfo.petalReferenceColumns
                .map { CodeTemplate("\n  %M::${it.name},", accessorClassInfo.entityMemberName) }
        }

        code ( "\n).$EXPORT_PETAL_METHOD_SIMPLE_NAME().eagerLoadDependencies()" )
    }
