package com.lyeeedar.Components

import com.lyeeedar.Board.MonsterEffect
import com.lyeeedar.Util.XmlData

inline fun Entity.monsterEffect(): MonsterEffectComponent? = this.components[ComponentType.MonsterEffect] as MonsterEffectComponent?
class MonsterEffectComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.MonsterEffect

	lateinit var monsterEffect: MonsterEffect

	fun set(monsterEffect: MonsterEffect): MonsterEffectComponent
	{
		this.monsterEffect = monsterEffect
		return this
	}

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{

	}
}