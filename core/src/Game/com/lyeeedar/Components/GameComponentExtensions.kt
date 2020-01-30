package com.lyeeedar.Components

import com.lyeeedar.Board.MonsterAI
import com.lyeeedar.Board.isMonster

fun Entity.isBasicOrb(): Boolean
{
	if (!this.hasComponent(ComponentType.Matchable)) return false

	if (this.hasComponent(ComponentType.Special)) return false

	if (this.matchable()!!.desc.isNamed) return false

	return true
}

fun Entity.niceName(): String
{
	val id = toString().split("Entity").last()
	val archetype = this.archetype()?.archetype?.toString() ?: "unknown"

	var name = "$archetype$id"

	if (this.nameOrNull() != null)
	{
		name += "(N:${this.name().name})"
	}

	if (isMonster())
	{
		val ai = this.ai()!!.ai as MonsterAI
		name += "(M:${ai.desc.name})"
	}

	if (special() != null)
	{
		name += "(S:${special()!!.special.getChar()})"
	}

	if (matchable() != null)
	{
		name += "(O:${matchable()!!.desc.name[0]})"
	}

	return name
}