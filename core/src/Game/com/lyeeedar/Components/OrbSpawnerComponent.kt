package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.Event1Arg
import com.lyeeedar.Util.XmlData

fun Entity.orbSpawner(): OrbSpawnerComponent? = OrbSpawnerComponent.mapper.get(this)
class OrbSpawnerComponent : AbstractComponent()
{
	val numToSpawnChanged = Event1Arg<Int>()

	var numToSpawn = 0
		set(value)
		{
			field = value
			numToSpawnChanged.invoke(numToSpawn)
		}

	var spacingCounter = 0

	var spawn: ((grid: Grid, entity: Entity)->Entity?)? = null

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<OrbSpawnerComponent> = ComponentMapper.getFor(OrbSpawnerComponent::class.java)
		fun get(entity: Entity): OrbSpawnerComponent? = mapper.get(entity)

		private val pool: Pool<OrbSpawnerComponent> = object : Pool<OrbSpawnerComponent>() {
			override fun newObject(): OrbSpawnerComponent
			{
				return OrbSpawnerComponent()
			}

		}

		@JvmStatic fun obtain(): OrbSpawnerComponent
		{
			val obj = OrbSpawnerComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { OrbSpawnerComponent.pool.free(this); obtained = false } }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset()
	{
		spawn = null
		numToSpawn = 0
		spacingCounter = 0
	}
}