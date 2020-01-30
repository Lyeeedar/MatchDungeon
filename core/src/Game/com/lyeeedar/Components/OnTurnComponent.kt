package com.lyeeedar.Components

import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.XmlData

fun Entity.onTurn(): OnTurnComponent? = this.components[ComponentType.OnTurn] as OnTurnComponent?
class OnTurnComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.OnTurn

	var onTurn: ((grid: Grid, entity: Entity)->Unit)? = null

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		onTurn = null
	}
}