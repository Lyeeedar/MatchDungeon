package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.tryGet

abstract class Swappable(val theme: Theme) : Point()
{
	lateinit var sprite: Sprite

	val movePoints = Array<Point>()
	var spawnCount = -1
	var cascadeCount = 0

	abstract val canMove: Boolean

	var sealSprite: Sprite = theme.sealSprites.tryGet(0).copy()
	var sealCount = 0
		set(value)
		{
			field = value
			sealSprite = theme.sealSprites.tryGet(sealCount-1).copy()
		}
	val sealed: Boolean
		get() = sealCount > 0

	var grid: Grid? = null

	var isChanger: Boolean = false

	var nextDesc: OrbDesc? = null
		set(value)
		{
			field = value
			nextSprite = sprite.copy()
			nextSprite!!.colour = nextDesc!!.sprite.colour.copy().a(0.75f)
			nextSprite!!.baseScale[0] = 1.25f
			nextSprite!!.baseScale[1] = 1.25f
		}
	var nextSprite: Sprite? = null
}

abstract class Matchable(theme: Theme) : Swappable(theme)
{
	abstract var desc: OrbDesc
	abstract val canMatch: Boolean
	abstract var markedForDeletion: Boolean
	abstract var deletionEffectDelay: Float
}