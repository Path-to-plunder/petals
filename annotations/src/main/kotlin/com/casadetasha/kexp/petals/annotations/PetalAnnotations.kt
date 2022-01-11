package com.casadetasha.kexp.petals.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.CLASS)
annotation class Petal(val tableName: String,
                       val version: Int = 1,
                       val primaryKeyType: PetalPrimaryKey = PetalPrimaryKey.NONE)

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class AlterColumn(val renameFrom: String = "")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD, AnnotationTarget.PROPERTY)
annotation class VarChar(val charLimit: Int)

enum class PetalPrimaryKey(val dataType: String?) {
    NONE(null),
    INT("SERIAL"),
    LONG("BIGSERIAL"),
    UUID("uuid")
}
