package com.lyeeedar.Components

import com.lyeeedar.Board.Grid
import com.lyeeedar.Util.XmlData

abstract class AbstractGridAI
{
	abstract fun onTurn(entity: Entity, grid: Grid)
}

inline fun Entity.ai(): AIComponent? = this.components[ComponentType.AI] as AIComponent?
class AIComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.AI

	lateinit var ai: AbstractGridAI

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) { }
}