package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.generationdsl.dsl.*
import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.asParameterTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.createNestedPetalPropertyTemplates
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions.createEagerLoadFunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions.createStoreDependenciesFunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions.createStoreFunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions.createTransactFunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.toConstructorPropertyTemplate
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

internal fun FileTemplate.generateAccessorClass(accessorClassInfo: AccessorClassInfo) =
    generateClass(accessorClassInfo.className) {
        generateAccessorConstructor(accessorClassInfo)
        generateAccessorSuperClass(accessorClassInfo)

        collectPropertyTemplates { createNestedPetalPropertyTemplates(accessorClassInfo) }

        collectFunctionTemplates {
            mutableSetOf(
                createStoreFunctionTemplate(accessorClassInfo),
                createStoreDependenciesFunctionTemplate(accessorClassInfo),
                createTransactFunctionTemplate(accessorClassInfo),
                createEagerLoadFunctionTemplate(accessorClassInfo),
            ).apply {
                addAll( createLoadReferencingPetalFunctionTemplate(accessorClassInfo) )
            }
        }

        generateCompanionObject {
            collectFunctionTemplates {
                mutableSetOf(
                    createCreateFunctionTemplate(accessorClassInfo),

                    createLoadFunctionTemplate(accessorClassInfo),
                    createLoadAllFunctionTemplate(accessorClassInfo),
                    createLazyLoadAllFunctionTemplate(accessorClassInfo),

                    createExportFunctionTemplate(accessorClassInfo)
                ).addIf(accessorClassInfo.petalColumns.any { it is PetalReferenceColumn }) {
                    createCompanionEagerLoadDependenciesFunctionTemplate(accessorClassInfo)
                }
            }
        }
    }

internal fun ClassTemplate.generateAccessorConstructor(accessorClassInfo: AccessorClassInfo) {
    generatePrimaryConstructor {
        collectConstructorPropertyTemplates(this@generateAccessorConstructor) {
            accessorClassInfo.petalValueColumns
                .map { it.toConstructorPropertyTemplate() }
        }

        collectParameterTemplates {
            accessorClassInfo.petalReferenceColumns
                .map { it.asParameterTemplate() }
                .toMutableList()
                .apply {
                    add(ParameterTemplate(name = "dbEntity", typeName = accessorClassInfo.entityClassName))
                    add(ParameterTemplate(name = "id", typeName = accessorClassInfo.idKotlinClassName))
                }
        }
    }
}

internal fun ClassTemplate.generateAccessorSuperClass(accessorClassInfo: AccessorClassInfo) {
    generateSuperClass(
        PetalAccessor::class.asClassName()
            .parameterizedBy(
                accessorClassInfo.className,
                accessorClassInfo.entityClassName,
                accessorClassInfo.idKotlinClassName
            )
    ) {
        generateConstructorParam { CodeTemplate("dbEntity, id") }
    }
}

private fun <E> MutableSet<E>.addIf(condition: Boolean, function: () -> E): MutableSet<E> = apply {
    if (condition) {
        add(function())
    }
}
