package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.EntityAccessor
import com.casadetasha.kexp.petals.processor.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {
        val classTypeBuilder = TypeSpec.classBuilder(accessorClassInfo.className)
            .addSuperclass(accessorClassInfo)
            .addEntityDelegate(accessorClassInfo)
            .addAccessorProperties(accessorClassInfo)

        classTypeBuilder
            .primaryConstructor(ConstructorSpecBuilder(accessorClassInfo).constructorSpec)
            .addStoreMethod(accessorClassInfo)
            .addAccessorCompanionObject(accessorClassInfo)

        return classTypeBuilder.build()
    }
}

// This will always be type EntityAccessor so asClassName() is safe here
@OptIn(DelicateKotlinPoetApi::class)
private fun TypeSpec.Builder.addSuperclass(accessorClassInfo: AccessorClassInfo) = apply {
    superclass(EntityAccessor::class.java.asClassName()
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

private fun TypeSpec.Builder.addAccessorCompanionObject(accessorClassInfo: AccessorClassInfo) {
    this.addType(
        TypeSpec
            .companionObjectBuilder()
            .addCreateMethod(accessorClassInfo)
            .addFunction(AccessorLoadFunSpecBuilder().getFunSpec(accessorClassInfo))
            .addFunction(AccessorExportFunSpecBuilder().getFunSpec(accessorClassInfo))
            .build()
    )
}

internal fun KClass<*>.asMemberName(): MemberName {
    val className = this.asClassName()
    return MemberName(packageName = className.packageName, simpleName = className.simpleName)
}
