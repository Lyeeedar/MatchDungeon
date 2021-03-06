package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Board.GridUpdate.Match
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.Statics

class Tile(x: Int, y: Int, val grid: Grid) : Point(x, y)
{
	var groundSprite: SpriteWrapper? = null
	var wallSprite: SpriteWrapper? = null

	var contents: Entity? = null
		set(value)
		{
			if (Statics.debug && value != null && !canHaveOrb && (value.hasComponent(ComponentType.Swappable) || value.hasComponent(ComponentType.AI)))
			{
				val wall = wallSprite != null
				throw RuntimeException("Tried to put something in tile ($x,$y) that should be empty. IsPit: $isPit, IsWall: $wall, Existing: $field, object: ${value.archetype()!!.archetype}")
			}
			field = value
		}

	var spreader: Spreader? = null

	var plateStrength: Int = 0
	val hasPlate: Boolean
		get () = plateStrength > 0

	var canHaveOrb: Boolean = true
	var isPit: Boolean = false

	var isSelected: Boolean = false

	var nameKey: String? = null

	val effects: Array<Renderable> = Array()

	fun addDelayedAction(function: (tile: Tile) -> Unit, delay: Float)
	{
		grid.queuedActions.add(DelayedAction.obtain().set(function, delay, this))
	}

	val associatedMatches = kotlin.Array<Match?>(2) { e -> null}

	override fun toString(): String
	{
		if (contents != null && contents!!.isBasicOrb()) return contents!!.matchable()!!.desc.name[0].toString()
		if (contents != null && contents!!.orbSpawner() != null)
		{
			if (contents!!.orbSpawner()!!.numToSpawn > 0 ) return "£"
			else return "$"
		}
		if (contents != null && contents!!.special() != null)
		{
			val special = contents!!.special()!!.special
			return special.getChar().toString()
		}
		if (spreader != null) return "s"
		if (isPit) return "~"
		if (!canHaveOrb) return "#"
		if (contents != null) return contents!!.archetype()?.archetype?.letter?.toString() ?: " "

		return " "
	}
}