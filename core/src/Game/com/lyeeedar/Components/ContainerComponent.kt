package com.lyeeedar.Components

import com.lyeeedar.Board.Tile
import com.lyeeedar.Util.XmlData

fun Tile.getContentsOrContainer(check: (entity: Entity)->Boolean): Entity?
{
	if (contents == null) return null

	if (check(contents!!)) return contents

	if (contents!!.container() != null && check(contents!!.container()!!.containedEntity!!))
	{
		return contents!!.container()!!.containedEntity!!
	}

	return null
}

inline fun Entity.container(): ContainerComponent? = this.components[ComponentType.Container] as ContainerComponent?
class ContainerComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Container

	var containedEntity: Entity? = null

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		containedEntity = null
	}
}