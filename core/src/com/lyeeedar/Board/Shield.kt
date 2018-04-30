package com.lyeeedar.Board

import com.lyeeedar.Util.tryGet

class Shield(theme: Theme) : Swappable(theme)
{
	override val canMove: Boolean
		get() = true

	var count: Int = 1
		get() = field
		set(value)
		{
			field = value
			sprite = theme.shieldSprites.tryGet(value-1).copy()
		}

	init
	{
		count = 1
	}
}