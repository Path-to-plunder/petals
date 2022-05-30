package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorEagerLoadDependenciesFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.addCreateMethod
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ClassTemplate.Companion.classTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate.Companion.collectConstructorProperties
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorTemplate.Companion.primaryConstructorTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate.Companion.collectFunctionTemplates
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate.Companion.collectParameterTemplates
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.PropertyTemplate.Companion.collectPropertyTemplates
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.SuperclassTemplate.Companion.constructorParamTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.SuperclassTemplate.Companion.superclassTemplate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

internal fun FileTemplate.accessorClassTemplate(accessorClassInfo: AccessorClassInfo) =
        classTemplate(accessorClassInfo.className) {
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

            primaryConstructorTemplate {
                collectConstructorProperties(this@classTemplate) {
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

            collectPropertyTemplates {
                createNestedPetalPropertyTemplates(accessorClassInfo)
            }

            collectFunctionTemplates {
                createLoadReferencingPetalFunctionTemplate(accessorClassInfo)
                    .toMutableList()
                    .apply {
                        add(createStoreFunctionTemplate(accessorClassInfo))
                        add(createStoreDependenciesFunSpec(accessorClassInfo))
                        add(createTransactFunctionTemplate(accessorClassInfo))

                        add(createEagerLoadMethod(accessorClassInfo))
                    }
            }

            performOnTypeBuilder {
//                addStoreMethod(accessorClassInfo)
//                addEagerLoadMethod(accessorClassInfo)
                addAccessorCompanionObject(accessorClassInfo)
            }
        }

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {

        return TypeSpec.classBuilder(accessorClassInfo.className)
//            .addSuperclass(accessorClassInfo)
//            .addAccessorProperties(accessorClassInfo)
//            .primaryConstructor(AccessorConstructorSpecBuilder(accessorClassInfo).constructorSpec)
//            .addNestedPetalPropertySpec(accessorClassInfo)
//            .addReferencingPetalPropertySpec(accessorClassInfo)
//            .addStoreMethod(accessorClassInfo)
//            .addEagerLoadMethod(accessorClassInfo)
            .addAccessorCompanionObject(accessorClassInfo)
            .build()
    }
}

private fun TypeSpec.Builder.addSuperclass(accessorClassInfo: AccessorClassInfo) = apply {
    superclass(
        PetalAccessor::class.asClassName()
        .parameterizedBy(
            accessorClassInfo.className,
            accessorClassInfo.entityClassName,
            accessorClassInfo.idKotlinClassName
        )
    )
    addSuperclassConstructorParameter(CodeBlock.of("dbEntity, id"))
}

internal fun ParameterSpec.Builder.addDefaultValue(defaultValue: DefaultPetalValue) = apply {
    if (!defaultValue.hasDefaultValue) return@apply

    when (defaultValue.typeName.copy(nullable = false)) {
        String::class.asClassName() -> defaultValue("%S", defaultValue.value)
        else -> defaultValue("%L", defaultValue.value)
    }
}

private fun TypeSpec.Builder.addAccessorCompanionObject(accessorClassInfo: AccessorClassInfo) = apply {
    this.addType(
        TypeSpec
            .companionObjectBuilder()
            .addCreateMethod(accessorClassInfo)
            .addFunctions(AccessorLoadFunSpecBuilder(accessorClassInfo).loadFunSpecs)
            .addFunction(AccessorExportFunSpecBuilder(accessorClassInfo).exportFunSpec)
            .apply {
                if (accessorClassInfo.petalColumns.any { it is PetalReferenceColumn }) {
                    addFunction(AccessorEagerLoadDependenciesFunSpecBuilder(accessorClassInfo).companionEagerLoadDependenciesFunSpec)
                }
            }
            .build()
    )
}
