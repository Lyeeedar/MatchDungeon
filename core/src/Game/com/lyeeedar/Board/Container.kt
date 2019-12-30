package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager

class Container(override var sprite: Sprite, hp: Int, val contents: Any) : Damageable(), IHasTurnEffect
{
	val death = AssetManager.loadParticleEffect("Hit").getParticleEffect()

	var alwaysShowHP: Boolean = false

	override val onTurnEffects: Array<TurnEffect> = Array()

	init
	{
		this.maxhp = hp
	}
}