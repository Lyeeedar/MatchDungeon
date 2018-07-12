package com.lyeeedar.Board

import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.tryGet

/**
 * Created by Philip on 08-Jul-16.
 */

class Block(val theme: Theme) : Damageable()
{
	override var sprite = theme.blockSprites.tryGet(0).copy()

	val death = AssetManager.loadParticleEffect("Hit")
}