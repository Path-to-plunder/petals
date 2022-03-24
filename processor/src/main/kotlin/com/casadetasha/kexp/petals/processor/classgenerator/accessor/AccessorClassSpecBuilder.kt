package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.processor.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {
        return TypeSpec.classBuilder(accessorClassInfo.className)
            .addSuperclass(accessorClassInfo)
            .addAccessorProperties(accessorClassInfo)
            .primaryConstructor(ConstructorSpecBuilder(accessorClassInfo).constructorSpec)
            .addNestedPetalPropertySpec(accessorClassInfo)
            .addStoreMethod(accessorClassInfo)
            .addEagerLoadMethod(accessorClassInfo)
            .addAccessorCompanionObject(accessorClassInfo)
            .build()
    }
}

private fun TypeSpec.Builder.addEagerLoadMethod(accessorClassInfo: AccessorClassInfo) = apply {
    addFunction(AccessorEagerLoadDependenciesFunSpecBuilder(accessorClassInfo).petalEagerLoadDependenciesFunSpec)
}

// This will always be type EntityAccessor so asClassName() is safe here
@OptIn(DelicateKotlinPoetApi::class)
private fun TypeSpec.Builder.addSuperclass(accessorClassInfo: AccessorClassInfo) = apply {
    superclass(PetalAccessor::class.java.asClassName()
        .parameterizedBy(
            accessorClassInfo.className,
            accessorClassInfo.entityClassName,
            accessorClassInfo.idKotlinClassName
        )
    )
    addSuperclassConstructorParameter(CodeBlock.of("dbEntity, id"))
}

internal fun ParameterSpec.Builder.addDefaultValueIfPresent(defaultValue: DefaultPetalValue?) = apply {
    defaultValue?.let {
        if (!it.hasDefaultValue) { return@let }

        when (it.typeName.copy(nullable = false)) {
            String::class.asClassName() -> defaultValue("%S", it.defaultValue)
            else -> defaultValue("%L", it.defaultValue)
        }
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
                if (accessorClassInfo.columns.any { it.isReferenceColumn }) {
                    addFunction(AccessorEagerLoadDependenciesFunSpecBuilder(accessorClassInfo).companionEagerLoadDependenciesFunSpec)
                }
            }
            .build()
    )
}
