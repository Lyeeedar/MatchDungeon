package com.lyeeedar.Components

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData

fun Entity.swappable(): SwappableComponent? = this.components[ComponentType.Swappable] as SwappableComponent?
class SwappableComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Swappable

	val movePoints = Array<Point>()
	var spawnCount = -1
	var cascadeCount = 0

	var canMove: Boolean = true
		get() = !sealed && field

	var sealCount = 0
		set(value)
		{
			field = value
		}
	val sealed: Boolean
		get() = sealCount > 0

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		movePoints.clear()
		spawnCount = -1
		cascadeCount = 0

		canMove = true

		sealCount = 0
	}
}