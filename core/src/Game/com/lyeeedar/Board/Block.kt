package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.tryGet

/**
 * Created by Philip on 08-Jul-16.
 */

class Block(val theme: Theme) : Damageable(), IHasTurnEffect
{
	override var sprite = theme.blockSprites.tryGet(0).copy()
	var alwaysShowHP: Boolean = false

	val death = AssetManager.loadParticleEffect("Hit").getParticleEffect()

	override val onTurnEffects: Array<TurnEffect> = Array()
}