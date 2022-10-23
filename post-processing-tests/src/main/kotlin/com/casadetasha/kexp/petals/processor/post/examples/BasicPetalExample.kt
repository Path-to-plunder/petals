package com.casadetasha.kexp.petals.processor.post.examples

import com.casadetasha.kexp.petals.BasicPetalEntity
import com.casadetasha.kexp.petals.BasicPetalTable
import com.casadetasha.kexp.petals.annotations.PetalAccessor
import java.util.UUID
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Unit
import kotlin.collections.List
import org.jetbrains.exposed.dao.load
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.mapLazy
import org.jetbrains.exposed.sql.transactions.transaction

public class BasicPetal(
  public var color: String?,
  public var count: Int,
  public var secondColor: String,
  public var sporeCount: Long,
  public var uuid: UUID,
  dbEntity: BasicPetalEntity,
  id: UUID,
) : PetalAccessor<BasicPetal, BasicPetalEntity, UUID>(dbEntity, id) {

  public override fun applyInsideTransaction(statement: BasicPetal.() -> Unit): BasicPetal =
      apply  {
     transaction { statement() } 
  }

  protected override fun eagerLoadDependenciesInsideTransaction(): BasicPetal = apply {
  }

  public companion object {
    public fun create(
      color: String?,
      count: Int,
      id: UUID? = null,
      secondColor: String,
      sporeCount: Long,
      uuid: UUID,
    ): BasicPetal = transaction {
      val storeValues: BasicPetalEntity.() -> Unit =  {
        this.color = color
        this.count = count
        this.secondColor = secondColor
        this.sporeCount = sporeCount
        this.uuid = uuid
      }

      return@transaction when (id)  {
        null -> BasicPetalEntity.new { storeValues() }
        else -> BasicPetalEntity.new(id) { storeValues() }
      }
    }.toPetal()

    public fun load(id: UUID, eagerLoad: Boolean = false): BasicPetal? = transaction {
      BasicPetalEntity.findById(id)
    }?.toPetal()

    public fun loadAll(): List<BasicPetal> = transaction {
      BasicPetalEntity.all().map { it.toPetal() }
    }

    public fun lazyLoadAll(): SizedIterable<BasicPetal> = BasicPetalEntity.all().mapLazy {
      it.toPetal()
    }

    public fun loadFromQuery(op: (BasicPetalTable) -> Op<Boolean>): List<BasicPetal> = transaction {
      BasicPetalEntity.find(op(BasicPetalTable)).map { it.toPetal() }
    }

    protected fun storeInsideOfTransaction(basicPetal: BasicPetal, updateNestedDependencies: Boolean): BasicPetal {
      if (updateNestedDependencies)  {
          storeDependencies(basicPetal)
      }

      return basicPetal.dbEntity.apply  {
          color = basicPetal.color
          count = basicPetal.count
          secondColor = basicPetal.secondColor
          sporeCount = basicPetal.sporeCount
          uuid = basicPetal.uuid
      }.toPetal()
    }

    private fun storeDependencies(basicPetal: BasicPetal): Unit {
    }
  }
}

public fun BasicPetalEntity.toPetal(): BasicPetal = BasicPetal(dbEntity = this,
    color = color,
    count = count,
    secondColor = secondColor,
    sporeCount = sporeCount,
    uuid = uuid,
    id = id.value,
)
