package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.*
import com.casadetasha.kexp.generationdsl.dsl.ClassTemplate
import com.casadetasha.kexp.generationdsl.dsl.ClassTemplate.Companion.classTemplate
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.CompanionObjectTemplate.Companion.companionObjectTemplate
import com.casadetasha.kexp.generationdsl.dsl.ConstructorPropertyTemplate.Companion.collectConstructorProperties
import com.casadetasha.kexp.generationdsl.dsl.ConstructorTemplate.Companion.primaryConstructorTemplate
import com.casadetasha.kexp.generationdsl.dsl.FileTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate.Companion.collectFunctionTemplates
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate.Companion.collectParameterTemplates
import com.casadetasha.kexp.generationdsl.dsl.PropertyTemplate.Companion.collectPropertyTemplates
import com.casadetasha.kexp.generationdsl.dsl.SuperclassTemplate.Companion.constructorParamTemplate
import com.casadetasha.kexp.generationdsl.dsl.SuperclassTemplate.Companion.superclassTemplate
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName

internal fun FileTemplate.accessorClassTemplate(accessorClassInfo: AccessorClassInfo) =
    classTemplate(accessorClassInfo.className) {
        accessorConstructorTemplate(accessorClassInfo)
        accessorSuperClassTemplate(accessorClassInfo)

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

        companionObjectTemplate {
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

internal fun ClassTemplate.accessorConstructorTemplate(accessorClassInfo: AccessorClassInfo) {
    primaryConstructorTemplate {
        collectConstructorProperties(this@accessorConstructorTemplate) {
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

internal fun ClassTemplate.accessorSuperClassTemplate(accessorClassInfo: AccessorClassInfo) {
    superclassTemplate(
        PetalAccessor::class.asClassName()
            .parameterizedBy(
                accessorClassInfo.className,
                accessorClassInfo.entityClassName,
                accessorClassInfo.idKotlinClassName
            )
    ) {
        constructorParamTemplate { CodeTemplate("dbEntity, id") }
    }
}

private fun <E> MutableSet<E>.addIf(condition: Boolean, function: () -> E): MutableSet<E> = apply {
    if (condition) {
        add(function())
    }
}
