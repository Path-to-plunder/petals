package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.ParsedPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataExportFunSpecBuilder.Companion.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.asMemberName
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ClassTemplate.Companion.classTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FileTemplate.Companion.fileTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate.Companion.functionTemplate
import com.squareup.kotlinpoet.AnnotationSpec
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
                modifiers = KModifier.DATA,
                annotations = AnnotationTemplate(Serializable::class)
            ) {
                primaryConstructorTemplate {
                    accessorClassInfo.petalColumns
                        .filterIsInstance<LocalPetalColumn>()
                        .onEach { column ->
                            parameterTemplate(
                                name = column.propertyName,
                                typeName = column.propertyTypeName
                            )
                        }
                    }

                accessorClassInfo.petalColumns
                    .filterIsInstance<LocalPetalColumn>()
                    .map { column ->
                        propertyTemplate(
                            name = column.propertyName,
                            typeName = column.propertyTypeName,
                            annotations = when (column.kotlinType) {
                                UUID::class.asClassName() -> listOf(uuidSerializableAnnotation)
                                else -> emptyList()
                            },
                            isMutable = column.isMutable,
                        )
                    }
            }

            functionTemplate(
                name = EXPORT_METHOD_SIMPLE_NAME,
                receiver = accessorClassInfo.className,
                typeName = accessorClassInfo.dataClassName
            ) {
                addCode("return ${accessorClassInfo.dataClassName.simpleName}(")

                accessorClassInfo.petalValueColumns.forEach {
                    addCode("\n  ${it.name} = ${it.name},")
                }

                accessorClassInfo.petalReferenceColumns
                    .forEach {
                        val nullibleState = if (it.isNullable) { "?" } else { "" }
                        addCode(
                            "\n  ${it.name}Id = readValues[%M.${it.name}]$nullibleState.value,",
                            accessorClassInfo.tableMemberName
                        )
                    }

                accessorClassInfo.localColumns.filterIsInstance<PetalIdColumn>()
                    .forEach {
                        addCode("\n  ${it.name} = ${it.name}.value,")
                    }
            }

            functionTemplate(
                name = EXPORT_METHOD_SIMPLE_NAME,
                receiver = accessorClassInfo.className,
                typeName = accessorClassInfo.dataClassName
            ) {
                addCode("return ${accessorClassInfo.dataClassName.simpleName}(")

                accessorClassInfo.localColumns.forEach{
                    addCode("\n  ${it.propertyName} = ${it.propertyName},")
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
private val uuidSerializableAnnotation: AnnotationSpec get() {
    return AnnotationSpec.builder(Serializable::class)
        .addMember("with = %M::class", UUIDSerializer::class.asMemberName())
        .build()
}
