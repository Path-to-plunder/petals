package com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data

import com.casadetasha.kexp.generationdsl.dsl.*
import com.casadetasha.kexp.petals.annotations.UUIDSerializer
import com.casadetasha.kexp.petals.processor.model.AccessorClassInfo
import com.casadetasha.kexp.petals.processor.model.columns.*
import com.casadetasha.kexp.petals.processor.model.columns.LocalPetalColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalIdColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalReferenceColumn
import com.casadetasha.kexp.petals.processor.model.columns.PetalValueColumn
import com.casadetasha.kexp.petals.processor.outputgenerator.renderer.data.DataClassTemplateValues.EXPORT_DATA_METHOD_SIMPLE_NAME
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.reflect.KClass

internal object DataClassTemplateValues {
    const val PACKAGE_NAME = "com.casadetasha.kexp.petals.data"
    const val EXPORT_DATA_METHOD_SIMPLE_NAME = "asData"
}

internal fun FileTemplate.createDataClassFromTemplate(accessorClassInfo: AccessorClassInfo) = apply {
        generateClass(
            className = accessorClassInfo.dataClassName,
            modifiers = listOf(KModifier.DATA),
            annotations = listOf(AnnotationTemplate(Serializable::class))
        ) {
            generatePrimaryConstructor {
                collectConstructorPropertyTemplates(this@generateClass) {
                    setOf(
                        accessorClassInfo.localColumns
                            .filter { it.isExportable }
                            .map { column -> column.createConstructorProperty()
                            },
                        accessorClassInfo.timestampColumns
                            .filter { it.isExportable }
                            .map { column -> column.createConstructorProperty()
                            },
                    ).flatten()
                }
            }
        }

        generateFunction(
            name = EXPORT_DATA_METHOD_SIMPLE_NAME,
            receiverType = accessorClassInfo.entityClassName,
            returnType = accessorClassInfo.dataClassName
        ) {
            generateMethodBody {
                generateParenthesizedCodeBlock("return ${accessorClassInfo.dataClassName.simpleName}") {
                    collectCodeTemplates {
                        setOf(
                            accessorClassInfo.localColumns
                                .filter { it.isExportable }
                                .map { column -> column.entityDataExportCode(accessorClassInfo) },
                            accessorClassInfo.timestampColumns
                                .filter { it.isExportable }
                                .map { column -> column.entityDataExportCode(accessorClassInfo) },
                        ).flatten()
                    }
                }
            }
        }

        generateFunction(
            name = EXPORT_DATA_METHOD_SIMPLE_NAME,
            receiverType = accessorClassInfo.className,
            returnType = accessorClassInfo.dataClassName
        ) {
            generateMethodBody {
                generateParenthesizedCodeBlock("return ${accessorClassInfo.dataClassName.simpleName}") {
                    collectCodeTemplates {
                        setOf(
                            accessorClassInfo.localColumns
                                .filter { it.isExportable }
                                .map { CodeTemplate("\n  ${it.propertyName} = ${it.propertyName},") },
                            accessorClassInfo.timestampColumns
                                .filter { it.isExportable }
                                .map { CodeTemplate("\n  ${it.propertyName} = ${it.propertyName},") }
                        ).flatten()
                    }
                }
            }
        }
    }

private fun LocalPetalColumn.entityDataExportCode(accessorClassInfo: AccessorClassInfo): CodeTemplate {
    return when (this) {
        is PetalValueColumn -> CodeTemplate("\n  $name = $name,")
        is PetalTimestampColumn -> CodeTemplate("\n  $name = $name,")
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

private fun LocalPetalColumn.createConstructorProperty(): ConstructorPropertyTemplate {
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
        generateMember("with = %M::class", UUIDSerializer::class.asMemberName())
    }
}

private fun KClass<*>.asMemberName(): MemberName = asClassName().let { MemberName(it.packageName, it.simpleName) }

