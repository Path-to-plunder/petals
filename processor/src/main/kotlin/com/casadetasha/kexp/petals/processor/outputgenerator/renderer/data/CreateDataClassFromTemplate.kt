package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.annotationparser.AnnotationParser.kaptKotlinGeneratedDir
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.accessor.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ClassTemplate.Companion.classTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate.Companion.collectConstructorProperties
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorTemplate.Companion.primaryConstructorTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataClassTemplateValues.EXPORT_METHOD_SIMPLE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataClassTemplateValues.PACKAGE_NAME
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.AnnotationTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.CodeTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.ConstructorPropertyTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FileTemplate.Companion.fileTemplate
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.dsl.FunctionTemplate.Companion.functionTemplate
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import kotlinx.serialization.Serializable
import java.util.*

internal object DataClassTemplateValues {
    const val PACKAGE_NAME = "com.casadetasha.kexp.petals.data"
    const val EXPORT_METHOD_SIMPLE_NAME = "asData"
}

internal fun createDataClassFromTemplate(accessorClassInfo: AccessorClassInfo) =
    fileTemplate(
        directory = kaptKotlinGeneratedDir,
        packageName = PACKAGE_NAME,
        fileName = accessorClassInfo.dataClassName.simpleName
    ) {
        classTemplate(
            name = accessorClassInfo.dataClassName,
            modifiers = listOf(KModifier.DATA),
            annotations = listOf(AnnotationTemplate(Serializable::class))
        ) {
            primaryConstructorTemplate {
                collectConstructorProperties(this@classTemplate) {
                    accessorClassInfo.localColumns
                        .map { column -> column.constructorProperty() }
                }
            }
        }

        functionTemplate(
            name = EXPORT_METHOD_SIMPLE_NAME,
            receiverType = accessorClassInfo.entityClassName,
            returnType = accessorClassInfo.dataClassName
        ) {
            parenthesizedBlock("return ${accessorClassInfo.dataClassName.simpleName}") {
                collectCode{
                    accessorClassInfo.localColumns
                        .map { column -> column.entityDataExportCode(accessorClassInfo) }
                }
            }
        }

        functionTemplate(
            name = EXPORT_METHOD_SIMPLE_NAME,
            receiverType = accessorClassInfo.className,
            returnType = accessorClassInfo.dataClassName
        ) {
            parenthesizedBlock("return ${accessorClassInfo.dataClassName.simpleName}") {
                collectCode {
                    accessorClassInfo.localColumns.map {
                        CodeTemplate("\n  ${it.propertyName} = ${it.propertyName},")
                    }
                }
            }
        }
    }

private fun LocalPetalColumn.entityDataExportCode(accessorClassInfo: AccessorClassInfo): CodeTemplate {
    return when (this) {
        is PetalValueColumn -> CodeTemplate("\n  $name = $name,")
        is PetalIdColumn -> CodeTemplate("\n  $name = ${name}.value,")
        is PetalReferenceColumn -> {
            val nullableState = if (isNullable) { "?" } else { "" }
            CodeTemplate(
                "\n  ${name}Id = readValues[%M.$name]$nullableState.value,",
                accessorClassInfo.tableMemberName
            )
        }
    }
}

private fun LocalPetalColumn.constructorProperty(): ConstructorPropertyTemplate {
    return ConstructorPropertyTemplate(
        name = propertyName,
        typeName = propertyTypeName,
        annotations = propertyAnnotations,
        isMutable = isMutable,
    )
}

private val LocalPetalColumn.propertyAnnotations: List<AnnotationTemplate>
    get() {
        return when (kotlinType) {
            UUID::class.asClassName() -> listOf(uuidSerializableAnnotation)
            else -> emptyList()
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

private val uuidSerializableAnnotation: AnnotationTemplate
    get() {
    return AnnotationTemplate(Serializable::class) {
        addMember("with = %M::class", UUIDSerializer::class.asMemberName())
    }
}

