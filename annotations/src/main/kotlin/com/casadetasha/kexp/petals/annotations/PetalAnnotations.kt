package com.casadetasha.kexp.petals.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Petal(val tableName: String,
                       val className: String,
                       val version: Int = 1,
                       val primaryKeyType: PetalPrimaryKey = PetalPrimaryKey.INT
)

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
