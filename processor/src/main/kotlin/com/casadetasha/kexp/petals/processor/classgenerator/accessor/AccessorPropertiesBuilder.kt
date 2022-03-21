package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.UnprocessedPetalColumn
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.util.*

internal class AccessorPropertiesBuilder(val accessorClassInfo: AccessorClassInfo) {

    val properties: Iterable<PropertySpec> by lazy {
        accessorClassInfo.sortedColumns.map { it.toPropertySpec() }
    }
}

internal fun TypeSpec.Builder.addAccessorProperties(accessorClassInfo: AccessorClassInfo) = apply {
    this.addProperty(getDbEntityPropertySpec(accessorClassInfo))
    this.addProperties(AccessorPropertiesBuilder(accessorClassInfo).properties)
}

private fun getDbEntityPropertySpec(accessorClassInfo: AccessorClassInfo): PropertySpec = PropertySpec
    .builder("dbEntity", accessorClassInfo.entityClassName.copy(nullable = true))
    .initializer("dbEntity")
    .addAnnotation(Transient::class)
    .build()

private fun UnprocessedPetalColumn.toPropertySpec(): PropertySpec {
    val propertyTypeName = kotlinType.copy(nullable = isNullable)
    val name = when (referencing) {
        null -> this.name
        else -> "${this.name}Id"
    }
    val propertyBuilder = PropertySpec.builder(name, propertyTypeName)
    val serialName = name

    if (!isId) {
        propertyBuilder.mutable()
    }

    if (serialName != name) {
        propertyBuilder.addAnnotation(
            AnnotationSpec.builder(SerialName::class)
                .addMember("%S", serialName)
                .build()
        )
    }

    if (kotlinType == UUID::class.asClassName()) {
        propertyBuilder.addAnnotation(
            AnnotationSpec.builder(Serializable::class)
                .addMember("with = %M::class", UUIDSerializer::class.asMemberName())
                .build()
        )
    }

    return propertyBuilder
        .initializer(name)
        .build()
}
