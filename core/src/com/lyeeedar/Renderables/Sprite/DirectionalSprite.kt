package com.lyeeedar.Renderables.Sprite

import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Direction
import com.lyeeedar.Util.FastEnumMap
import ktx.collections.set

class DirectionalSprite
{
	private val sprites = ObjectMap<String, FastEnumMap<Direction, Sprite>>()
	private val availableAnimations = ObjectSet<String>()

	var size: Int
		get() = -1
		set(value)
		{
			for (anim in sprites)
			{
				for (sprite in anim.value)
				{
					sprite.size[0] = value
					sprite.size[1] = value
				}
			}
		}

	fun hasAnim(anim: String) = availableAnimations.contains(anim)

	fun addAnim(name: String, up: Sprite, down: Sprite, left: Sprite, right: Sprite)
	{
		if (availableAnimations.contains(name)) throw RuntimeException("Tried to add a duplicate animation for '$name'!")

		val map = FastEnumMap<Direction, Sprite>(Direction::class.java)
		sprites[name] = map

		map[Direction.NORTH] = up
		map[Direction.SOUTH] = down
		map[Direction.EAST] = right
		map[Direction.WEST] = left

		availableAnimations.add(name)
	}

	fun getSprite(anim: String, dir: Direction): Sprite
	{
		val sprite = sprites[anim] ?: throw RuntimeException("Failed to find direction sprite for $anim!")
		return sprite[dir] ?: throw RuntimeException("Failed to find direction $dir on anim $anim!")
	}
}