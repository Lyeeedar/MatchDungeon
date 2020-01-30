package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

fun Entity.onTurnEffect(): OnTurnEffectComponent? = this.components[ComponentType.OnTurnEffect] as OnTurnEffectComponent?
class OnTurnEffectComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.OnTurnEffect

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}
}