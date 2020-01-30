package com.lyeeedar.Components

import com.lyeeedar.Board.OrbDesc
import com.lyeeedar.Util.XmlData

inline fun Entity.matchable(): MatchableComponent? = this.components[ComponentType.Matchable] as MatchableComponent?
class MatchableComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Matchable

	var desc: OrbDesc = OrbDesc()
		private set(value)
		{
			field = value
		}

	fun setDesc(desc: OrbDesc, entity: Entity)
	{
		this.desc = desc
		entity.renderable().renderable.colour = desc.sprite.colour
	}

	var canMatch: Boolean = true
	var markedForDeletion: Boolean = false
	var deletionEffectDelay: Float = 0f

	var isChanger: Boolean = false
		private set(value)
		{
			field = value
		}
	var nextDesc: OrbDesc? = null

	fun setIsChanger(nextDesc: OrbDesc)
	{
		isChanger = true
		this.nextDesc = nextDesc
	}

	var skipPowerOrb = false

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		isChanger = false
		nextDesc = null
		canMatch = true
		markedForDeletion = false
		deletionEffectDelay = 0f
		skipPowerOrb = false
	}
}