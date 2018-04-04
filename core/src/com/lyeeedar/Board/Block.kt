package com.lyeeedar.Board

import com.lyeeedar.Game.Theme
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.tryGet

/**
 * Created by Philip on 08-Jul-16.
 */

class Block(val theme: Theme)
{
	var sprite = theme.blockSprites.tryGet(0).copy()

	val death = AssetManager.loadParticleEffect("Hit")
	var count = 1
		set(value)
		{
			field = value
			sprite = theme.blockSprites.tryGet(count-1).copy()
		}
}