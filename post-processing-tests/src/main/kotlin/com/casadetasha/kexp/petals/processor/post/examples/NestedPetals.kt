package com.casadetasha.kexp.petals.processor.post.examples

import java.util.UUID
import kotlin.String
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column

public object NestedPetalClassTable : UUIDTable(name = "nested_petal") {
    public val name: Column<String> = text("name")
}

public class NestedPetalClassEntity(
    id: EntityID<UUID>
) : UUIDEntity(id) {
    public var name: String by NestedPetalClassTable.name

    public companion object : UUIDEntityClass<NestedPetalClassEntity>(NestedPetalClassTable)
}
