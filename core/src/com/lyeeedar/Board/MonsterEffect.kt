package com.lyeeedar.Board

import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager

enum class MonsterEffectType
{
	ATTACK,
	HEAL,
	SUMMON
}

class MonsterEffect(val effect: MonsterEffectType, val data: ObjectMap<String, Object>, desc: OrbDesc, theme: Theme) : Matchable(theme)
{
	override var desc: OrbDesc = OrbDesc()
		set(value)
		{
			field = value
			sprite.colour = desc.sprite.colour
		}
	val actualSprite: Sprite

	override val canMatch: Boolean
		get() = true

	override var markedForDeletion: Boolean = false
	override var deletionEffectDelay: Float = 0f

	override val canMove: Boolean
		get() = !sealed

	var timer: Int = -1
	var delayDisplay: Float = 0f

	init
	{
		actualSprite = when (effect)
		{
			MonsterEffectType.ATTACK -> AssetManager.loadSprite("Oryx/uf_split/uf_items/skull_small", drawActualSize = true)
			else -> throw Exception("Unhandled monster effect type '$effect'!")
		}
		sprite = desc.sprite.copy()

		this.desc = desc
	}

	fun apply(grid: Grid)
	{
		when(effect)
		{
			MonsterEffectType.ATTACK -> applyAttack(grid)
			else -> throw Exception("Unhandled monster effect type '$effect'!")
		}
	}

	fun applyAttack(grid: Grid)
	{
		grid.onAttacked(this)
	}
}