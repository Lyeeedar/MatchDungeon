package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Components.DamageableComponent
import com.lyeeedar.Components.SwappableComponent
import com.lyeeedar.Components.archetype
import com.lyeeedar.Components.hasComponent
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.Point

/**
 * Created by Philip on 04-Jul-16.
 */

class Tile(x: Int, y: Int, val grid: Grid) : Point(x, y), IHasTurnEffect
{
	var groundSprite: SpriteWrapper? = null
	var wallSprite: SpriteWrapper? = null

	var contents: Entity? = null
		set(value)
		{
			if (value != null && !canHaveOrb && (value.hasComponent(SwappableComponent::class.java) || value.hasComponent(DamageableComponent::class.java)))
			{
				System.err.print("Tried to put something in tile that should be empty. IsPit: $isPit, Existing: $field, object: $value")
				return
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
	val delayedActions: Array<DelayedAction> = Array()

	fun addDelayedAction(function: () -> Unit, delay: Float)
	{
		delayedActions.add(DelayedAction(function, delay))
	}

	val associatedMatches = kotlin.Array<Match?>(2) {e -> null}

	override val onTurnEffects: Array<TurnEffect> = Array()

	override fun toString(): String
	{
		if (spreader != null) return "s"
		if (isPit) return "~"
		if (!canHaveOrb) return "#"
		if (contents != null) return contents!!.archetype()?.archetype?.letter?.toString() ?: " "

		return " "
	}
}

class DelayedAction(val function: () -> Unit, var delay: Float)