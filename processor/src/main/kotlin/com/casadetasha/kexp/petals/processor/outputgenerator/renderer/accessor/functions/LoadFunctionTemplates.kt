package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions

import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadMethods.LAZY_LOAD_ALL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadMethods.LOAD_ALL_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadMethods.LOAD_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadMethods.MAP_LAZY_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.CreateMethodNames.TRANSACTION_MEMBER_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.EagerLoadDependenciesMethodNames.COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.ExportMethodNames.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate.Companion.collectParameterTemplates
import com.squareup.kotlinpoet.MemberName
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
    return when (petalColumns.any { it is PetalReferenceColumn }) {
        true -> CodeTemplate {
            controlFlow("return %M", TRANSACTION_MEMBER_NAME) {
                controlFlow("when (eagerLoad)") {
                    codeTemplate {
                        CodeTemplate(
                            format = "true -> %M.findById(id)?.$COMPANION_EAGER_LOAD_DEPENDENCIES_METHOD_SIMPLE_NAME()",
                            entityMemberName
                        )
                    }

                    codeTemplate {
                        CodeTemplate("false -> %M.findById(id)?.$EXPORT_METHOD_SIMPLE_NAME()", entityMemberName)
                    }
                }
            }
        }
        false -> CodeTemplate {
            controlFlow(
                prefix = "return %M", TRANSACTION_MEMBER_NAME,
                suffix = "?.$EXPORT_METHOD_SIMPLE_NAME()"
            ) {
                codeTemplate { CodeTemplate("%M.findById(id)", entityMemberName) }
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
                controlFlow("return %M", TRANSACTION_MEMBER_NAME) {
                    codeTemplate {
                        CodeTemplate(
                            "%M.all().map { it.$EXPORT_METHOD_SIMPLE_NAME() }",
                            accessorClassInfo.entityMemberName
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
                "return %M.all().%M { it.$EXPORT_METHOD_SIMPLE_NAME() }",
                accessorClassInfo.entityMemberName,
                MAP_LAZY_MEMBER_NAME
            )
        }
    }

private object AccessorLoadMethods {
    const val LOAD_METHOD_SIMPLE_NAME = "load"
    const val LOAD_ALL_METHOD_SIMPLE_NAME = "loadAll"
    const val LAZY_LOAD_ALL_METHOD_SIMPLE_NAME = "lazyLoadAll"
    val MAP_LAZY_MEMBER_NAME = MemberName("org.jetbrains.exposed.sql", "mapLazy")
}
