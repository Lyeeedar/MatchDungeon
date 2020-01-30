package com.lyeeedar.Components

import com.lyeeedar.Board.Special
import com.lyeeedar.Util.XmlData

fun Entity.special(): SpecialComponent? = this.components[ComponentType.Special] as SpecialComponent?
class SpecialComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Special

	lateinit var special: Special

	fun set(special: Special): SpecialComponent
	{
		this.special = special
		return this
	}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}
}