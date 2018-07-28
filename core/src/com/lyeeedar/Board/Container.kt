package com.lyeeedar.Board

import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager

class Container(override var sprite: Sprite, hp: Int, val contents: Any) : Damageable()
{
	val death = AssetManager.loadParticleEffect("Hit")

	var alwaysShowHP: Boolean = false

	init
	{
		this.maxhp = hp
	}
}