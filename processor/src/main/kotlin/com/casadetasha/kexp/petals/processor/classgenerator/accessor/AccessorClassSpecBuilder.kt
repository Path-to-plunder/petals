package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.ktx.kotlinType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
            .addIsStoredParam()
            .addAccessorCompanionObject(accessorClassInfo)

        return classTypeBuilder.build()
    }

    private fun getAccessorPropertySpec(property: PetalColumn): PropertySpec {
        val propertyTypeName = property.kotlinType
        val propertyBuilder = PropertySpec.builder(property.name, propertyTypeName)
        val serialName = property.name;

        if (!property.isId!!) { propertyBuilder.mutable() }

        if (serialName != property.name) {
            propertyBuilder.addAnnotation(
                AnnotationSpec.builder(SerialName::class)
                    .addMember("%S", serialName)
                    .build()
            )
        }

        if (property.kotlinType == UUID::class.asClassName()) {
            propertyBuilder.addAnnotation(AnnotationSpec.builder(Serializable::class)
                .addMember("with = %M::class", UUIDSerializer::class.asMemberName())
                .build())
        }

        return propertyBuilder
            .initializer(property.name)
            .build()
    }

    private fun createConstructorSpec(petalColumns: Set<PetalColumn>): FunSpec {
        val constructorBuilder = FunSpec.constructorBuilder()
        petalColumns.forEach {
            constructorBuilder.addParameter(getPropertySpec(it))
        }
        return constructorBuilder.build()
    }

    private fun getPropertySpec(column: PetalColumn): ParameterSpec {
        val propertyTypeName = column.kotlinType
        return ParameterSpec.builder(column.name, propertyTypeName)
            .build()
    }

}

private fun TypeSpec.Builder.addIsStoredParam() = apply {
    addProperty(
        PropertySpec.builder("isStored_", Boolean::class.asClassName(), KModifier.PRIVATE)
            .addAnnotation(Transient::class)
            .initializer("false")
            .mutable()
            .build())
    addProperty(
        PropertySpec.builder("isStored", Boolean::class.asClassName(), KModifier.PUBLIC)
            .getter(FunSpec.getterBuilder()
                .addAnnotation(Synchronized::class)
                .addStatement("return isStored_")
                .build()
            )
            .mutable()
            .setter(FunSpec.setterBuilder()
                .addParameter("value", Boolean::class.asClassName())
                .addModifiers(KModifier.PRIVATE)
                .addAnnotation(Synchronized::class)
                .addStatement("isStored_ = value")
                .build()
            )
            .build())
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
