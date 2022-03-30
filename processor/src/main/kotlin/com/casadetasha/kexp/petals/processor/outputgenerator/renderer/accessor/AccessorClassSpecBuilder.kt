package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor

import com.casadetasha.kexp.petals.annotations.PetalAccessor
import com.casadetasha.kexp.petals.processor.inputparser.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.*
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorEagerLoadDependenciesFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorExportFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.AccessorLoadFunSpecBuilder
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.functions.addCreateMethod
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassSpecBuilder(val accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) {

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

private fun TypeSpec.Builder.addEagerLoadMethod(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
    addFunction(AccessorEagerLoadDependenciesFunSpecBuilder(accessorClassInfo).petalEagerLoadDependenciesFunSpec)
}

// This will always be type EntityAccessor so asClassName() is safe here
@OptIn(DelicateKotlinPoetApi::class)
private fun TypeSpec.Builder.addSuperclass(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
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

private fun TypeSpec.Builder.addAccessorCompanionObject(accessorClassInfo: com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo) = apply {
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
