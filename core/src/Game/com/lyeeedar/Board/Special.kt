package com.lyeeedar.Board

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Animation.*
import com.lyeeedar.Renderables.HSLColour
import com.lyeeedar.Renderables.Light
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*
import java.util.*

/**
 * Created by Philip on 30-Jul-16.
 */

val beamMoveSpeed = 0.1f
val lightTemplate = Light(Colour.WHITE, brightness = 0.3f, range = 2f)

fun addSpecial(entity: Entity, special: Special): Entity
{
	var holder = entity.special()
	if (holder == null)
	{
		entity.addComponent(ComponentType.Special)
		holder = entity.special()!!
	}
	holder.special = special

	val renderable = special.sprite.copy()
	renderable.light = lightTemplate.copy()
	renderable.tintLight = true
	renderable.colour = entity.matchable()?.desc?.sprite?.colour ?: Colour.WHITE

	if (special is GemSpecial)
	{
		renderable.colourAnimation = ChromaticAnimation.obtain().set(15f)

		entity.removeComponent(ComponentType.Matchable)
	}
	else
	{
		renderable.colourAnimation = BlinkAnimation.obtain().set(renderable.colour, 0.1f, 2.5f, false)
	}

	entity.renderable()?.renderable = renderable

	return entity
}

fun tryMergeSpecial(special: Special, entity: Entity): Special
{
	val otherSpecial = entity.special()
	if (otherSpecial != null)
	{
		val tempEntity = EntityPool.obtain()
		tempEntity.addComponent(ComponentType.Special)
		tempEntity.special()!!.set(special)

		val newSpecial = otherSpecial.special.merge(tempEntity) ?: special.merge(entity) ?: special

		tempEntity.free()

		return newSpecial
	}
	else
	{
		return special
	}
}

abstract class Special()
{
	var needsArming = false
	var armed: Boolean = false
		private set(value)
		{
			field = value
		}

	fun setArmed(armed: Boolean, entity: Entity)
	{
		this.armed = armed

		val swappable = entity.swappable()
		if (swappable != null)
		{
			swappable.canMove = !armed
		}

		val matchable = entity.matchable()
		if (matchable != null)
		{
			matchable.canMatch = !armed
		}
	}

	fun setNeedsArming(): Special
	{
		needsArming = true
		return this
	}

	lateinit var sprite: Sprite

	val uniqueID = UUID.randomUUID().toString()

	abstract fun merge(other: Entity): Special?
	abstract fun apply(point: Point, grid: Grid)
	abstract fun copy(): Special
	abstract fun getChar(): Char

	companion object
	{
		fun modifyColour(colour: Colour): Colour
		{
			val col = colour * 0.8f
			col.a = 1f
			val lerped = col.copy().lerp(Colour.WHITE, 0.3f)
			return lerped
		}

		fun mix(colour1: Colour, colour2: Colour): Colour
		{
			val hsl1 = HSLColour(colour1)
			val hsl2 = HSLColour(colour2)

			val out = HSLColour()
			out.l = 0.6f
			out.s = 1.0f
			out.h = hsl1.h.lerp(hsl2.h, 0.5f)

			return out.toRGB()
		}

		fun popColumn(special: Special, colour: Colour, x: Int, sy: Int, grid: Grid)
		{
			if (x < 0 || x >= grid.width) return

			fun launchAt(x: Int, y: Int)
			{
				val p2 = Vector2(x.toFloat(), sy.toFloat())
				val p1 = Vector2(x.toFloat(), y.toFloat())

				val dist = p1.dst(p2)

				val effect = AssetManager.loadParticleEffect("SpecialBeam").getParticleEffect()
				effect.colour = modifyColour(colour)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(dist * beamMoveSpeed * grid.animSpeedMultiplier, arrayOf(p1, p2), Interpolation.linear)
				effect.rotation = getRotation(p1, p2)
				grid.grid[x, y].effects.add(effect)

				for (hy in min(y, sy)..max(y, sy))
				{
					val dist = Vector2(x.toFloat(), sy.toFloat()).dst(Vector2(x.toFloat(), hy.toFloat()))
					val duration = dist * beamMoveSpeed * grid.animSpeedMultiplier

					grid.grid[x, hy].addDelayedAction(
						{ tile ->
							grid.pop(tile, 0f, special.uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
						}, duration)
				}
			}

			var launchedUp = false
			for (y in sy + 1 until grid.height)
			{
				val tile = grid.grid[x, y]
				if (!tile.canHaveOrb && !tile.isPit)
				{
					launchedUp = true
					launchAt(x, y)

					break
				}
			}
			if (!launchedUp)
			{
				launchAt(x, grid.height-1)
			}

			var launchedDown = false
			for (y in sy-1 downTo 0)
			{
				val tile = grid.grid[x, y]
				if (!tile.canHaveOrb && !tile.isPit)
				{
					launchedDown = true
					launchAt(x, y)

					break
				}

			}
			if (!launchedDown)
			{
				launchAt(x, 0)
			}
		}

		fun popRow(special: Special, colour: Colour, sx: Int, y: Int, grid: Grid)
		{
			if (y < 0 || y >= grid.height) return

			fun launchAt(x: Int, y: Int)
			{
				val p1 = Vector2(sx.toFloat(), y.toFloat())
				val p2 = Vector2(x.toFloat(), y.toFloat())

				val dist = p1.dst(p2)

				val effect = AssetManager.loadParticleEffect("SpecialBeam").getParticleEffect()
				effect.colour = modifyColour(colour)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(dist * beamMoveSpeed * grid.animSpeedMultiplier, arrayOf(p1, p2), Interpolation.linear)
				effect.rotation = getRotation(p1, p2)
				grid.grid[x, y].effects.add(effect)

				for (hx in min(x, sx)..max(x, sx))
				{
					val dist = Vector2(sx.toFloat(), y.toFloat()).dst(Vector2(hx.toFloat(), y.toFloat()))
					val duration = dist * beamMoveSpeed * grid.animSpeedMultiplier

					grid.grid[hx, y].addDelayedAction(
						{ tile ->
							grid.pop(tile, 0f, special.uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
						}, duration)
				}
			}

			var launchedRight = false
			for (x in sx+1 until grid.width)
			{
				val tile = grid.grid[x, y]
				if (!tile.canHaveOrb && !tile.isPit)
				{
					launchedRight = true
					launchAt(x, y)

					break
				}
			}
			if (!launchedRight)
			{
				launchAt(grid.width-1, y)
			}

			var launchedLeft = false
			for (x in sx-1 downTo 0)
			{
				val tile = grid.grid[x, y]
				if (!tile.canHaveOrb && !tile.isPit)
				{
					launchedLeft = true
					launchAt(x, y)

					break
				}
			}
			if (!launchedLeft)
			{
				launchAt(0, y)
			}
		}
	}
}

abstract class BeamSpecial() : Special()

abstract class HorizontalBeamSpecial() : BeamSpecial()
{
	override fun getChar(): Char = '|'
}

abstract class VerticalBeamSpecial() : BeamSpecial()
{
	override fun getChar(): Char = '-'
}

class Cross() : BeamSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_hori_vert", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		popColumn(this, sprite.colour, point.x, point.y, grid)
		popRow(this, sprite.colour, point.x, point.y, grid)
	}

	override fun merge(other: Entity): Special?
	{
		return null
	}

	override fun copy(): Special
	{
		return Cross()
	}

	override fun getChar(): Char = '+'
}

class Horizontal4() : HorizontalBeamSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_vert", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		popColumn(this, sprite.colour, point.x, point.y, grid)
	}

	override fun merge(other: Entity): Special?
	{
		val otherSpecial = other.special()?.special

		if (otherSpecial is Horizontal4 || otherSpecial is Vertical4)
		{
			return Cross().setNeedsArming()
		}

		return null
	}

	override fun copy(): Special
	{
		return Horizontal4()
	}
}

class Vertical4() : VerticalBeamSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_hori", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		popRow(this, sprite.colour, point.x, point.y, grid)
	}

	override fun merge(other: Entity): Special?
	{
		val otherSpecial = other.special()?.special
		if (otherSpecial is Horizontal4 || otherSpecial is Vertical4)
		{
			return Cross().setNeedsArming()
		}

		return null
	}

	override fun copy(): Special
	{
		return Vertical4()
	}
}

abstract class BombSpecial() : Special()
{
	override fun getChar(): Char = '*'
}

class DoubleDualMatch() : BombSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_dual_dual", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		val coreTile = grid.tile(point)!!

		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.colour = modifyColour(sprite.colour)
		effect.size[0] = 4
		effect.size[1] = 4
		effect.isCentered = true

		coreTile.addDelayedAction(
			{
				for (tile in grid.grid)
				{
					if (tile.dist(point) < 4)
					{
						grid.pop(tile.x, tile.y, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
					}
				}
			}, 0.075f)

		coreTile.effects.add(effect)
	}

	override fun merge(other: Entity): Special?
	{
		return null
	}

	override fun copy(): Special
	{
		return DoubleDualMatch()
	}
}

class DualHori() : HorizontalBeamSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_dual_vert", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		popColumn(this, sprite.colour, point.x-1, point.y, grid)
		popColumn(this, sprite.colour, point.x, point.y, grid)
		popColumn(this, sprite.colour, point.x+1, point.y, grid)
	}

	override fun merge(other: Entity): Special?
	{
		return null
	}

	override fun copy(): Special
	{
		return DualHori()
	}
}

class DualVert() : VerticalBeamSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_dual_hori", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		popRow(this, sprite.colour, point.x, point.y-1, grid)
		popRow(this, sprite.colour, point.x, point.y, grid)
		popRow(this, sprite.colour, point.x, point.y+1, grid)
	}

	override fun merge(other: Entity): Special?
	{
		return null
	}

	override fun copy(): Special
	{
		return DualVert()
	}
}

class DualMatch() : BombSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_dual", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		val coreTile = grid.tile(point)!!

		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.colour = modifyColour(sprite.colour)
		effect.size[0] = 3
		effect.size[1] = 3
		effect.isCentered = true

		coreTile.addDelayedAction(
			{
				for (tile in grid.grid)
				{
					if (tile.dist(point) < 3)
					{
						grid.pop(tile.x, tile.y, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
					}
				}
			}, 0.075f)

		coreTile.effects.add(effect)
	}

	override fun merge(other: Entity): Special?
	{
		val otherSpecial = other.special()?.special
		if (otherSpecial is DualMatch)
		{
			return DoubleDualMatch().setNeedsArming()
		}
		else if (otherSpecial is Horizontal4)
		{
			return DualHori().setNeedsArming()
		}
		else if (otherSpecial is Vertical4)
		{
			return DualVert().setNeedsArming()
		}

		return null
	}

	override fun copy(): Special
	{
		return DualMatch()
	}
}

abstract class GemSpecial() : Special()
{
	var targetDesc: OrbDesc? = null

	override fun getChar(): Char = '@'
}

class Match5() : GemSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/gem", drawActualSize = true)
	}

	override fun merge(other: Entity): Special?
	{
		val otherSpecial = other.special()?.special

		if (otherSpecial is Special)
		{
			if (otherSpecial is GemSpecial)
			{
				return Match5Dual().setNeedsArming()
			}
			else
			{
				val newSpecial = Match5Spread(otherSpecial)
				newSpecial.targetDesc = other.matchable()!!.desc
				newSpecial.needsArming = true
				return newSpecial
			}
		}
		else if (other.matchable() != null)
		{
			targetDesc = other.matchable()!!.desc
			needsArming = true

			return this
		}

		return null
	}

	override fun apply(point: Point, grid: Grid)
	{
		sprite.colourAnimation = null
		sprite.colour = (targetDesc ?: OrbDesc.getValidOrbs(grid.level).random()).sprite.colour.copy()

		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.size[0] = 2
		effect.size[1] = 2
		effect.isCentered = true
		effect.colour = sprite.colour

		grid.tile(point)?.effects?.add(effect)

		fun isValidTarget(tile: Tile): Boolean
		{
			val contents = tile.contents ?: return false

			if (contents.matchable()?.desc?.key == targetDesc?.key) return true

			if (contents.damageable() != null && contents.ai() != null) return true

			if (contents.damageable() != null && contents.damageable()!!.maxhp > 1) return true

			return false
		}

		val hitSet = ObjectSet<Tile>()
		for (tile in grid.grid.filter { isValidTarget(it) })
		{
			val dst = tile.dist(point)
			val animDuration = 0.275f + dst * 0.05f

			val diff = tile.getPosDiff(point)
			diff[0].y *= -1

			val s = AssetManager.loadSprite("Oryx/Custom/items/shard")
			s.faceInMoveDirection = true
			s.colour = Colour.random(s = 0.5f, l = 0.9f)
			s.drawActualSize = true
			s.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.5f)
			s.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.3f, false)
			tile.effects.add(s)

			tile.addDelayedAction(
				{ tile ->
					grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
				}, animDuration)
		}
	}

	override fun copy(): Special
	{
		return Match5()
	}
}

class Match5Spread(val special: Special) : GemSpecial()
{
	init
	{
		if (special is HorizontalBeamSpecial)
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_vert", drawActualSize = true)
		}
		else if (special is VerticalBeamSpecial)
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_hori", drawActualSize = true)
		}
		else if (special is BombSpecial)
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_dual", drawActualSize = true)
		}
		else
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem", drawActualSize = true)
		}
	}

	override fun merge(other: Entity): Special?
	{
		val otherSpecial = other.special()?.special

		if (otherSpecial is GemSpecial)
		{
			return Match5Dual().setNeedsArming()
		}
		else if (other.matchable() != null)
		{
			targetDesc = other.matchable()!!.desc
			needsArming = true

			return this
		}

		return null
	}

	override fun apply(point: Point, grid: Grid)
	{
		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.size[0] = 2
		effect.size[1] = 2
		effect.isCentered = true
		effect.colour = sprite.colour

		grid.tile(point)?.effects?.add(effect)

		for (tile in grid.grid)
		{
			val contents = tile.contents ?: continue

			if (contents.matchable()?.desc == targetDesc)
			{
				val dst = tile.dist(point)
				val animDuration = 0.275f + dst * 0.05f

				val diff = tile.getPosDiff(point)
				diff[0].y *= -1

				val s = special.sprite.copy()
				s.drawActualSize = false
				s.faceInMoveDirection = true
				s.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.5f)
				s.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.0f, false)
				tile.effects.add(s)

				tile.addDelayedAction(
					{ tile ->
						if (contents.matchable() != null)
						{
							val newSpecial =
								if (contents.special() == null)
									special.copy()
								else
								{
									val specialHolder = EntityPool.obtain()
									specialHolder.addComponent(ComponentType.Special)
									specialHolder.special()!!.set(special)

									val merged = contents.special()!!.special.merge(specialHolder) ?: special.merge(contents) ?: special

									specialHolder.free()

									merged
								}

							addSpecial(contents, newSpecial)
							newSpecial.setArmed(true, contents)
						}

						grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
					}, animDuration)
			}
			else if (
				(contents.damageable() != null && contents.ai() != null) ||
				(contents.damageable() != null && contents.damageable()!!.maxhp > 1))
			{
				val dst = tile.dist(point)
				val animDuration = 0.275f + dst * 0.05f

				val diff = tile.getPosDiff(point)
				diff[0].y *= -1

				val s = AssetManager.loadSprite("Oryx/Custom/items/shard")
				s.faceInMoveDirection = true
				s.colour = Colour.random(s = 0.5f, l = 0.9f)
				s.drawActualSize = true
				s.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.5f)
				s.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.3f, false)
				tile.effects.add(s)

				tile.addDelayedAction(
					{ tile ->
						grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
					}, animDuration)
			}
		}
	}

	override fun copy(): Special
	{
		return Match5Spread(special)
	}
}

class Match5Dual() : GemSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_gem", drawActualSize = true)
	}

	override fun merge(other: Entity): Special?
	{
		return this
	}

	override fun apply(point: Point, grid: Grid)
	{
		sprite.colourAnimation = null
		sprite.colour = Colour.random()

		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.size[0] = 2
		effect.size[1] = 2
		effect.isCentered = true
		effect.colour = sprite.colour

		grid.tile(point)?.effects?.add(effect)

		for (tile in grid.grid)
		{
			val dst = tile.dist(point)
			val animDuration = 0.275f + dst * 0.05f

			val diff = tile.getPosDiff(point)
			diff[0].y *= -1

			val s = AssetManager.loadSprite("Oryx/Custom/items/shard")
			s.faceInMoveDirection = true
			s.colour = Colour.random(s = 0.5f, l = 0.9f)
			s.drawActualSize = true
			s.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.5f)
			s.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.3f, false)
			tile.effects.add(s)

			tile.addDelayedAction(
				{ tile ->
					grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
				}, animDuration)
		}
	}

	override fun copy(): Special
	{
		return Match5Dual()
	}
}