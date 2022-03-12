package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.DefaultPetalValue
import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
internal class AccessorClassSpecBuilder(val accessorClassInfo: AccessorClassInfo) {

    internal fun getClassSpec(): TypeSpec {
        val classTypeBuilder = TypeSpec.classBuilder(accessorClassInfo.className)
            .addAnnotation(Serializable::class)
            .addEntityDelegate(accessorClassInfo)
            .addAccessorProperties(accessorClassInfo)

        classTypeBuilder
            .primaryConstructor(ConstructorSpecBuilder(accessorClassInfo).constructorSpec)
            .addDeleteMethod()
            .addStoreMethod(accessorClassInfo)
            .addAccessorCompanionObject(accessorClassInfo)

        return classTypeBuilder.build()
    }

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
