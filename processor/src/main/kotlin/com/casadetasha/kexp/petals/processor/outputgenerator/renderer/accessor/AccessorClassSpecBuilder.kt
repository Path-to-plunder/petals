package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorEagerLoadDependenciesFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.addCreateMethod
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {
        return TypeSpec.classBuilder(accessorClassInfo.className)
            .addSuperclass(accessorClassInfo)
            .addAccessorProperties(accessorClassInfo)
            .primaryConstructor(AccessorConstructorSpecBuilder(accessorClassInfo).constructorSpec)
            .addNestedPetalPropertySpec(accessorClassInfo)
            .addReferencingPetalPropertySpec(accessorClassInfo)
            .addStoreMethod(accessorClassInfo)
            .addEagerLoadMethod(accessorClassInfo)
            .addAccessorCompanionObject(accessorClassInfo)
            .build()
    }
}

private fun TypeSpec.Builder.addEagerLoadMethod(accessorClassInfo: AccessorClassInfo) = apply {
    addFunction(AccessorEagerLoadDependenciesFunSpecBuilder(accessorClassInfo).petalEagerLoadDependenciesFunSpec)
}

private fun TypeSpec.Builder.addSuperclass(accessorClassInfo: AccessorClassInfo) = apply {
    superclass(PetalAccessor::class.asClassName()
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
