package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.Point

/**
 * Created by Philip on 04-Jul-16.
 */

class Tile(x: Int, y: Int) : Point(x, y)
{
	val sprites = Array<SpriteWrapper>()

	var spriteSetter: SpriteWrapper? = null
		set(value)
		{
			sprites.add(value)
		}

	var orb: Orb?
		get() = contents as? Orb
		set(value) { contents = value }

	var sinkable: Sinkable?
		get() = contents as? Sinkable
		set(value) { contents = value }

	var swappable: Swappable?
		get() = contents as? Swappable
		set(value) { contents = value }

	var block: Block?
		get() = contents as? Block
		set(value) { contents = value }

	var chest: Chest?
		get() = contents as? Chest
		set(value) { contents = value }

	var creature: Creature?
		get() = contents as? Creature
		set(value) { contents = value }

	var damageable: Damageable?
		get() = contents as? Damageable
		set(value) { contents = value }

	var monster: Monster?
		get() = contents as? Monster
		set(value) { contents = value }

	var friendly: Friendly?
		get() = contents as? Friendly
		set(value) { contents = value }

	var container: Container?
		get() = contents as? Container
		set(value) { contents = value }

	var contents: Any? = null
		set(value)
		{
			if (value != null && !canHaveOrb)
			{
				System.err.print("Tried to put something in tile that should be empty. IsPit: $isPit, Existing: $field, object: $value")
				return
			}
			field = value
		}

	var plateStrength: Int = 0
	val hasPlate: Boolean
		get () = plateStrength > 0

	var canHaveOrb: Boolean = true
	var isPit: Boolean = false

	var isSelected: Boolean = false

	val effects: Array<Renderable> = Array()

	val associatedMatches = kotlin.Array<Match?>(2) {e -> null}

	override fun toString(): String
	{
		if (orb != null) return "o"
		if (sinkable != null) return "c"
		if (block != null) return "="
		if (monster != null) return "!"
		if (friendly != null) return "?"
		if (chest != null) return "£"
		if (isPit) return "~"
		if (!canHaveOrb) return "#"

		return " "
	}
}
