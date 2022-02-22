package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.classgenerator.accessor.functions.*
import com.casadetasha.kexp.petals.processor.ktx.kotlinType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.reflect.KClass

@OptIn(KotlinPoetMetadataPreview::class)
class AccessorClassSpecBuilder {

    internal fun getClassSpec(accessorClassInfo: AccessorClassInfo): TypeSpec {
        val classTypeBuilder = TypeSpec.classBuilder(accessorClassInfo.className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(Serializable::class)

        val tableColumns: Set<PetalColumn> = accessorClassInfo.columns.toSortedSet()

        tableColumns.forEach {
            classTypeBuilder.addProperty(getAccessorPropertySpec(it))
        }

        classTypeBuilder
            .primaryConstructor(createConstructorSpec(tableColumns))
            .addDeleteMethod()
            .addStoreMethod(accessorClassInfo)
            .addFindEntityMethod(accessorClassInfo)
            .addAccessorCompanionObject(accessorClassInfo)

        return classTypeBuilder.build()
    }

    private fun getAccessorPropertySpec(column: PetalColumn): PropertySpec {
        val propertyTypeName = when (column.isId!!) {
            true -> column.kotlinType.copy(nullable = true)
            false -> column.kotlinType.copy(nullable = column.isNullable)
        }
        val propertyBuilder = PropertySpec.builder(column.name, propertyTypeName)
        val serialName = column.name

        if (!column.isId!!) {
            propertyBuilder.mutable()
        }

        if (serialName != column.name) {
            propertyBuilder.addAnnotation(
                AnnotationSpec.builder(SerialName::class)
                    .addMember("%S", serialName)
                    .build()
            )
        }

        if (column.kotlinType == UUID::class.asClassName()) {
            propertyBuilder.addAnnotation(AnnotationSpec.builder(Serializable::class)
                .addMember("with = %M::class", UUIDSerializer::class.asMemberName())
                .build())
        }

        return propertyBuilder
            .initializer(column.name)
            .build()
    }

    private fun createConstructorSpec(petalColumns: Set<PetalColumn>): FunSpec {
        val constructorBuilder = FunSpec.constructorBuilder()
        petalColumns.forEach {
            constructorBuilder.addParameter(getParameterSpec(it))
        }
        return constructorBuilder.build()
    }

    private fun getParameterSpec(column: PetalColumn): ParameterSpec {
        val propertyTypeName = when (column.isId!!) {
            true -> column.kotlinType.copy(nullable = true)
            false -> column.kotlinType.copy(nullable = column.isNullable)
        }

        val builder = ParameterSpec.builder(column.name, propertyTypeName)
        return when (column.isId!!) {
            true -> builder.defaultValue("null").build()
            false -> builder.build()
        }
    }
}

private fun TypeSpec.Builder.addAccessorCompanionObject(accessorClassInfo: AccessorClassInfo) {
    this.addType(
        TypeSpec
            .companionObjectBuilder()
            .addFunction(AccessorLoadFunSpecBuilder().getFunSpec(accessorClassInfo))
            .addFunction(AccessorExportFunSpecBuilder().getFunSpec(accessorClassInfo))
            .build()
    )
}

private fun KClass<*>.asMemberName(): MemberName {
    val className = this.asClassName()
    return MemberName(packageName = className.packageName, simpleName = className.simpleName)
}
