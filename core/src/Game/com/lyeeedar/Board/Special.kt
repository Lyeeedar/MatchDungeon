package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
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
		holder = SpecialComponent.obtain()
		entity.add(holder)
	}
	holder.special = special

	val renderable = special.sprite.copy()
	renderable.light = lightTemplate.copy()
	renderable.tintLight = true
	renderable.colour = entity.matchable()!!.desc.sprite.colour

	if (special is GemSpecial)
	{
		renderable.colourAnimation = ChromaticAnimation.obtain().set(15f)
	}
	else
	{
		renderable.colourAnimation = BlinkAnimation.obtain().set(renderable.colour, 0.1f, 2.5f, false)
	}

	entity.renderable().renderable = renderable

	return entity
}

abstract class Special()
{
	var armed = false

	lateinit var sprite: Sprite

	val uniqueID = UUID.randomUUID().toString()

	abstract fun merge(other: Entity): Special?
	abstract fun apply(point: Point, grid: Grid)
	abstract fun copy(): Special

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

				val hitSet = ObjectSet<Tile>()

				val effect = AssetManager.loadParticleEffect("SpecialBeam").getParticleEffect()
				effect.colour = modifyColour(colour)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(dist * beamMoveSpeed * grid.animSpeedMultiplier, arrayOf(p1, p2), Interpolation.linear)
				effect.rotation = getRotation(p1, p2)
				effect.collisionFun = fun(cx: Int, pcy: Int)
				{
					val cy = (grid.height-1) - pcy
					val tile = grid.tile(cx, cy)
					val min = min(y, sy)
					val max = max(y, sy)
					if (tile != null && cx == x && cy in min..max && !hitSet.contains(tile))
					{
						hitSet.add(tile)
						grid.pop(cx, cy, 0f, special.uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
					}
				}
				grid.grid[x, y].effects.add(effect)
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

				val hitSet = ObjectSet<Tile>()

				val effect = AssetManager.loadParticleEffect("SpecialBeam").getParticleEffect()
				effect.colour = modifyColour(colour)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(dist * beamMoveSpeed * grid.animSpeedMultiplier, arrayOf(p1, p2), Interpolation.linear)
				effect.rotation = getRotation(p1, p2)
				effect.collisionFun = fun(cx: Int, pcy: Int)
				{
					val cy = (grid.height-1) - pcy
					val tile = grid.tile(cx, cy)
					val min = min(x, sx)
					val max = max(x, sx)
					if (tile != null && cy == y && cx in min..max && !hitSet.contains(tile))
					{
						hitSet.add(tile)
						grid.pop(cx, cy, 0f, special.uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
					}

				}
				grid.grid[x, y].effects.add(effect)
			}

			var launchedRight = false
			for (x in sx+1..grid.width-1)
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

abstract class VerticalBeamSpecial() : BeamSpecial()

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
			return Cross()
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
			return Cross()
		}

		return null
	}

	override fun copy(orbDesc: OrbDesc): Special
	{
		return Vertical4()
	}
}

abstract class BombSpecial() : Special()

class DoubleDualMatch() : BombSpecial()
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_dual_dual", drawActualSize = true)
	}

	override fun apply(point: Point, grid: Grid)
	{
		val coreTile = grid.tile(point)

		val hitSet = ObjectSet<Tile>()

		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.colour = modifyColour(sprite.colour)
		effect.size[0] = 4
		effect.size[1] = 4
		effect.isCentered = true
		effect.collisionFun = fun(cx: Int, pcy: Int)
		{

			val cy = (grid.height-1) - pcy
			val tile = grid.tile(cx, cy)
			if (tile != null && !hitSet.contains(tile) && tile.dist(point) < 4)
			{
				hitSet.add(tile)
				grid.pop(cx, cy, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
			}
		}

		Future.call({
						for (tile in grid.grid)
						{
							if (!hitSet.contains(tile) && tile.dist(point) < 4)
							{
								hitSet.add(tile)
								grid.pop(tile.x, tile.y, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
							}
						}
					}, 0.1f)

		coreTile?.effects?.add(effect)
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
		val coreTile = grid.tile(point)

		val hitSet = ObjectSet<Tile>()

		val effect = AssetManager.loadParticleEffect("SpecialExplosion").getParticleEffect()
		effect.colour = modifyColour(sprite.colour)
		effect.size[0] = 3
		effect.size[1] = 3
		effect.isCentered = true
		effect.collisionFun = fun(cx: Int, pcy: Int)
		{

			val cy = (grid.height-1) - pcy
			val tile = grid.tile(cx, cy)
			if (tile != null && !hitSet.contains(tile) && tile.dist(point) < 3)
			{
				hitSet.add(tile)
				grid.pop(cx, cy, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
			}
		}

		Future.call({
						for (tile in grid.grid)
						{
							if (!hitSet.contains(tile) && tile.dist(point) < 3)
							{
								hitSet.add(tile)
								grid.pop(tile.x, tile.y, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
							}
						}
					}, 0.1f)

		coreTile?.effects?.add(effect)
	}

	override fun merge(other: Entity): Special?
	{
		val otherSpecial = other.special()?.special
		if (otherSpecial is DualMatch)
		{
			return DoubleDualMatch()
		}
		else if (otherSpecial is Horizontal4)
		{
			return DualHori()
		}
		else if (otherSpecial is Vertical4)
		{
			return DualVert()
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
				return Match5Dual()
			}
			else
			{
				return Match5Spread()
			}
		}
		else if (other.matchable() != null)
		{
			targetDesc = other.matchable()!!.desc
			armed = true

			return this
		}

		return null
	}

	override fun apply(point: Point, grid: Grid)
	{
		sprite.colourAnimation = null
		sprite.colour = targetDesc!!.sprite.colour.copy()

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
			s.completionCallback = fun()
			{
				grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
			}
			tile.effects.add(s)
		}
	}

	override fun copy(): Special
	{
		return Match5()
	}
}

class Match5Spread(orbDesc: OrbDesc, theme: Theme, val special: Special) : GemSpecial(orbDesc, theme)
{
	init
	{
		if (special is HorizontalBeamSpecial)
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_vert", drawActualSize = true)
			sprite.colourAnimation = ChromaticAnimation.obtain().set(15f)
		}
		else if (special is VerticalBeamSpecial)
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_hori", drawActualSize = true)
			sprite.colourAnimation = ChromaticAnimation.obtain().set(15f)
		}
		else if (special is BombSpecial)
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_dual", drawActualSize = true)
			sprite.colourAnimation = ChromaticAnimation.obtain().set(15f)
		}
		else
		{
			sprite = AssetManager.loadSprite("Oryx/Custom/items/gem", drawActualSize = true)
			sprite.colourAnimation = ChromaticAnimation.obtain().set(15f)
		}

		targetDesc = special.desc
		sprite.colourAnimation = null
		sprite.colour = targetDesc!!.sprite.colour

		sprite.light = lightTemplate.copy()
		sprite.tintLight = true
	}

	override fun merge(other: Swappable): Special?
	{
		if (other is Special)
		{
			if (other is GemSpecial)
			{
				return Match5Dual(desc, theme)
			}
		}
		else if (other is Matchable)
		{
			targetDesc = other.desc

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
			if (tile.matchable?.desc == targetDesc)
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
				s.completionCallback = fun()
				{
					if (tile.matchable == null)
					{

					}
					else if (tile.special == null)
					{
						tile.special = special.copy(targetDesc!!)
					}
					else
					{
						val newspecial = tile.special!!.merge(special) ?: special.merge(tile.special!!) ?: special
						tile.special = newspecial
						newspecial.armed = true
					}

					grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
				}
				tile.effects.add(s)
			}
			else if (tile.creature != null || (tile.damageable?.maxhp ?: 0) > 1)
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
				s.completionCallback = fun()
				{
					grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
				}
				tile.effects.add(s)
			}
		}
	}

	override fun copy(orbDesc: OrbDesc): Special
	{
		return Match5Spread(orbDesc, theme, special)
	}
}

class Match5Dual(orbDesc: OrbDesc, theme: Theme) : GemSpecial(orbDesc, theme)
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/gem_gem", drawActualSize = true)
		sprite.colourAnimation = ChromaticAnimation.obtain().set(15f)
		sprite.light = lightTemplate.copy()
		sprite.tintLight = true
	}

	override fun merge(other: Swappable): Special?
	{
		if (other is Matchable)
		{
			return this
		}
		else if (other is Special)
		{
			return this
		}

		return null
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
			s.completionCallback = fun()
			{
				grid.pop(tile, 0f, uniqueID, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1, grid.level.player.getStat(Statistic.PIERCE))
			}
			tile.effects.add(s)
		}
	}

	override fun copy(orbDesc: OrbDesc): Special
	{
		return Match5Dual(orbDesc, theme)
	}
}