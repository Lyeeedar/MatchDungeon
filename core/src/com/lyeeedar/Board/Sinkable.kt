package com.lyeeedar.Board

import com.lyeeedar.Game.Theme
import com.lyeeedar.Renderables.Sprite.Sprite

class Sinkable : Swappable
{
	override val canMove: Boolean
		get() = !sealed

	constructor(sprite: Sprite, theme: Theme)
		: super(theme)
	{
		this.sprite = sprite
	}


}
