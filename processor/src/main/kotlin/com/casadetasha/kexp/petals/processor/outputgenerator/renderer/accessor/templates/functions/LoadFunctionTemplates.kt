package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.templates.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.generationdsl.dsl.CodeTemplate
import com.casadetasha.kexp.generationdsl.dsl.FunctionTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate
import com.casadetasha.kexp.generationdsl.dsl.ParameterTemplate.Companion.collectParameterTemplates
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.LAZY_LOAD_ALL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.LOAD_ALL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.LOAD_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.LoadMethodNames.MAP_LAZY_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.EagerLoadDependenciesMethodNames.COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.ExportMethodNames.EXPORT_PETAL_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.asClassName
import org.jetbrains.exposed.sql.SizedIterable

internal fun createLoadFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = LOAD_METHOD_SIMPLE_NAME,
        returnType = accessorClassInfo.className.copy(nullable = true),
    ) {
        collectParameterTemplates { accessorClassInfo.getLoadMethodParameters() }

        this.methodBody {
            accessorClassInfo.getLoadMethodBody()
        }
    }

private fun AccessorClassInfo.getLoadMethodParameters(): List<ParameterTemplate> {
    return listOf(
        ParameterTemplate(name = "id", typeName = idKotlinClassName),
        ParameterTemplate(name = "eagerLoad", typeName = Boolean::class.asClassName()) {
            defaultValue { CodeTemplate("false") }
        }
    )
}

private fun AccessorClassInfo.getLoadMethodBody(): CodeTemplate {
    val entitySimpleName = entityClassName.simpleName
    return when (petalColumns.any { it is PetalReferenceColumn }) {
        true -> CodeTemplate {
            controlFlowCode("return %M", TRANSACTION_MEMBER_NAME) {
                controlFlowCode("when (eagerLoad)") {
                    codeTemplate {
                        CodeTemplate(
                            format = "true -> %L.findById(id)?.$COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME()\n",
                            entitySimpleName
                        )
                    }

                    codeTemplate {
                        CodeTemplate("false -> %L.findById(id)?.$EXPORT_PETAL_METHOD_SIMPLE_NAME()\n", entitySimpleName)
                    }
                }
            }
        }
        false -> CodeTemplate {
            controlFlowCode(
                prefix = "return %M", TRANSACTION_MEMBER_NAME,
                suffix = "?.$EXPORT_PETAL_METHOD_SIMPLE_NAME()"
            ) {
                codeTemplate { CodeTemplate("%L.findById(id)\n", entitySimpleName) }
            }
        }
    }
}


internal fun createLoadAllFunctionTemplate(accessorClassInfo: AccessorClassInfo): FunctionTemplate =
    FunctionTemplate(
        name = LOAD_ALL_METHOD_SIMPLE_NAME,
        returnType = List::class.asClassName()
            .parameterizedBy(accessorClassInfo.className)
    ) {
        methodBody {
            CodeTemplate {
                controlFlowCode("return %M", TRANSACTION_MEMBER_NAME) {
                    codeTemplate {
                        CodeTemplate(
                            "%L.all().map { it.$EXPORT_PETAL_METHOD_SIMPLE_NAME() }\n",
                            accessorClassInfo.entityClassName.simpleName
                        )
                    }
                }
            }
        }
    }


internal fun createLazyLoadAllFunctionTemplate(accessorClassInfo: AccessorClassInfo) =
    FunctionTemplate(
        name = LAZY_LOAD_ALL_METHOD_SIMPLE_NAME,
        returnType = SizedIterable::class.asClassName()
            .parameterizedBy(accessorClassInfo.className)
    ) {
        methodBody {
            CodeTemplate(
                "return %L.all().%M·{ it.$EXPORT_PETAL_METHOD_SIMPLE_NAME() }\n",
                accessorClassInfo.entityClassName.simpleName,
                MAP_LAZY_MEMBER_NAME
            )
        }
    }
