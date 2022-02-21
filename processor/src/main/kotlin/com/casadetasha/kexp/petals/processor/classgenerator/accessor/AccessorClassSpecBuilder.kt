package com.casadetasha.kexp.petals.processor.classgenerator.accessor

import com.casadetasha.kexp.annotationparser.AnnotationParser
import com.casadetasha.kexp.petals.annotations.PetalColumn
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@OptIn(KotlinPoetMetadataPreview::class)
class AccessorClassSpecBuilder {

    internal fun getClassSpec(accessorClassInfo: AccessorClassInfo): TypeSpec {
        val classTypeBuilder = TypeSpec.classBuilder(accessorClassInfo.className)
            .addModifiers(KModifier.DATA)
            .addAnnotation(Serializable::class)

        val tableColumns: Set<PetalColumn> = accessorClassInfo.columns.toSortedSet()

//        val tableColumns: List<PetalColumn> = accessorClassInfo.columns.toList()

        tableColumns.forEach {
            classTypeBuilder.addProperty(getKexportedPropertySpec(it))
        }

        return classTypeBuilder
            .primaryConstructor(createConstructorSpec(tableColumns))
            .build()
    }

    private fun getKexportedPropertySpec(property: PetalColumn): PropertySpec {
        val propertyTypeName = property.kotlinType
        val propertyBuilder = PropertySpec.builder(property.name, propertyTypeName)
        val serialName = property.name;

        if (serialName != property.name) {
            propertyBuilder.addAnnotation(
                AnnotationSpec.builder(SerialName::class)
                    .addMember("%S", serialName)
                    .build()
            )
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

    private fun getPropertySpec(property: PetalColumn): ParameterSpec {
        val propertyTypeName = property.kotlinType
        return ParameterSpec.builder(property.name, propertyTypeName)
            .build()
    }

    private val PetalColumn.kotlinType: ClassName
        get() {
            if (dataType.startsWith("CHARACTER VARYING")) {
                return String::class.asClassName()
            }

            return when (val type = dataType) {
                "uuid" -> UUID::class
                "TEXT" -> String::class
                "INT" -> Int::class
                "BIGINT" -> Long::class
                else -> AnnotationParser.printThenThrowError(
                    "INTERNAL LIBRARY ERROR: unsupported datatype ($type) found while" +
                            " parsing column for accessor"
                )
            }.asClassName()
        }
}
