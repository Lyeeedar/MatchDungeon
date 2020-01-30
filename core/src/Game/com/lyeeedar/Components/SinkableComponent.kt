package com.lyeeedar.Components

import com.lyeeedar.Util.XmlData

inline fun Entity.sinkable(): SinkableComponent? = this.components[ComponentType.Sinkable] as SinkableComponent?
class SinkableComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Sinkable

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}
}