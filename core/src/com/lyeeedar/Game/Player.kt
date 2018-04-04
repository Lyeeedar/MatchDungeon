package com.lyeeedar.Game

import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.UI.UnlockTree

/**
 * Created by Philip on 15-Jul-16.
 */

class Player()
{
	val maxhp: Int
		get() = 5 + (maxhpStat * 2.5f).toInt()
	var maxhpStat: Int = 0

	val powerGain: Int
		get() = powerGainStat
	var powerGainStat: Int = 0

	val attackDam: Int
		get() = 1 + attackDamStat
	var attackDamStat: Int = 0

	val abilityDam: Int
		get() = 3 + abilityDamStat
	var abilityDamStat: Int = 0

	var gold: Int = 500

	// abilities and stuff
	val abilities = Array<Ability?>(4){ e -> null}

	val abilityTree: UnlockTree<Ability> = UnlockTree.load("UnlockTrees/Fire", {Ability()})

	fun getAbility(name: String?): Ability?
	{
		if (name == null) return null

		val skill = abilityTree.boughtDescendants()[name]
		return skill
	}
}