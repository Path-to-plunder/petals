package com.casadetasha.kexp.petals.annotations

import kotlin.reflect.KClass

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Petal(val tableName: String = "",
                             val className: String,
                             val primaryKeyType: PetalPrimaryKey = PetalPrimaryKey.INT)

annotation class PetalSchema(val petal: KClass<out Any>, val version: Int = 1)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultString(val value: String)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultInt(val value: Int)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class DefaultLong(val value: Long)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AlterColumn(val renameFrom: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class VarChar(val charLimit: Int)

enum class PetalPrimaryKey(val dataType: String?) {
    INT("SERIAL"),
    LONG("BIGSERIAL"),
    UUID("uuid")
}
