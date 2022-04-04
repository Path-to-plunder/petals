package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.asMemberName
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ClassTemplate.Companion.classTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorTemplate.Companion.primaryConstructorTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FileTemplate.Companion.fileTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate.Companion.functionTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ParameterTemplate.Companion.collectParameters
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.PropertyTemplate.Companion.collectProperties
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import kotlinx.serialization.Serializable
import java.util.*

@OptIn(KotlinPoetMetadataPreview::class)
internal class DslDataClassFileGenerator(
    private val accessorClassInfo: AccessorClassInfo
) {

    fun generateFile() {
        fileTemplate(
            directory = kaptKotlinGeneratedDir,
            packageName = PACKAGE_NAME,
            fileName = "${accessorClassInfo.className.simpleName}Data"
        ) {
            classTemplate(
                name = accessorClassInfo.dataClassName,
                modifiers = listOf(KModifier.DATA),
                annotations = listOf(AnnotationTemplate(Serializable::class))
            ) {
                primaryConstructorTemplate {
                    collectParameters {
                        accessorClassInfo.localColumns
                            .map { column ->
                                ParameterTemplate(
                                    name = column.propertyName,
                                    typeName = column.propertyTypeName
                                )
                            }
                    }
                }

                collectProperties {
                    accessorClassInfo.localColumns
                        .map { column ->
                            PropertyTemplate(
                                name = column.propertyName,
                                typeName = column.propertyTypeName,
                                annotations = when (column.kotlinType) {
                                    UUID::class.asClassName() -> listOf(uuidSerializableAnnotation)
                                    else -> emptyList()
                                },
                                isMutable = column.isMutable,
                                isParameter = true,
                            )
                        }
                }

                functionTemplate(
                    name = EXPORT_METHOD_SIMPLE_NAME,
                    receiverType = accessorClassInfo.entityClassName,
                    returnType = accessorClassInfo.dataClassName
                ) {
                    parenthesisedBlock("return ${accessorClassInfo.dataClassName.simpleName}") {
                        collectCode {
                            accessorClassInfo.petalValueColumns.map { CodeTemplate("\n  ${it.name} = ${it.name},") }
                        }

                        collectCode {
                            accessorClassInfo.petalReferenceColumns
                                .map {
                                    val nullableState = if (it.isNullable) { "?" } else { "" }
                                    CodeTemplate(
                                        "\n  ${it.name}Id = readValues[%M.${it.name}]$nullableState.value,",
                                        accessorClassInfo.tableMemberName
                                    )
                                }
                        }

                        collectCode {
                            accessorClassInfo.localColumns.filterIsInstance<PetalIdColumn>()
                                .map { CodeTemplate("\n  ${it.name} = ${it.name}.value,") }

                        }
                    }
                }

                functionTemplate(
                    name = EXPORT_METHOD_SIMPLE_NAME,
                    receiverType = accessorClassInfo.className,
                    returnType = accessorClassInfo.dataClassName
                ) {
                    writeCode("return ${accessorClassInfo.dataClassName.simpleName}(")

                    accessorClassInfo.localColumns.forEach {
                        writeCode("\n  ${it.propertyName} = ${it.propertyName},")
                    }

                    writeCode("\n)")
                }
            }

        }.writeToDisk()
    }

    companion object {
        const val PACKAGE_NAME = "com.casadetasha.kexp.petals.data"
    }
}

private val LocalPetalColumn.propertyTypeName: TypeName
    get() {
        return when (this) {
            is PetalIdColumn -> this.kotlinType
            else -> this.kotlinType.copy(nullable = this.isNullable)
        }
    }
private val LocalPetalColumn.propertyName: String
    get() {
        return when (this) {
            is PetalReferenceColumn -> "${this.name}Id"
            else -> this.name
        }
    }
private val uuidSerializableAnnotation: AnnotationTemplate get() {
    return AnnotationTemplate(Serializable::class) {
        addMember("with = %M::class", UUIDSerializer::class.asMemberName())
    }
}
