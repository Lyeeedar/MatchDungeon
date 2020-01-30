package com.lyeeedar.Components

import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.Event1Arg
import com.lyeeedar.Util.XmlData

inline fun Entity.orbSpawner(): OrbSpawnerComponent? = this.components[ComponentType.OrbSpawner] as OrbSpawnerComponent?
class OrbSpawnerComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.OrbSpawner

	var canSpawnSinkables = true

	val numToSpawnChanged = Event1Arg<Int>()

	var numToSpawn = 0
		set(value)
		{
			field = value
			numToSpawnChanged.invoke(numToSpawn)
		}

	var spacingCounter = 0

	lateinit var spawn: ((grid: Grid, entity: Entity)->Entity?)

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		numToSpawn = 0
		spacingCounter = 0
		canSpawnSinkables = true
		numToSpawnChanged.clear()
	}
}