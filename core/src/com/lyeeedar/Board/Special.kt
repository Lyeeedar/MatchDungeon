package com.lyeeedar.Board

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Animation.ChromaticAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Util.*

/**
 * Created by Philip on 30-Jul-16.
 */

val beamMoveSpeed = 0.1f

abstract class Special(val orb: Orb)
{
	lateinit var sprite: Sprite

	abstract fun merge(other: Orb): ((point: Point, grid: Grid, orb: Orb) -> Unit)?
	abstract fun apply(): (point: Point, grid: Grid, orb: Orb) -> Unit
	abstract fun copy(orb: Orb): Special

	companion object
	{
		fun modifyColour(colour: Colour): Colour
		{
			val col = colour * 0.8f
			val lerped = col.lerp(Colour.WHITE, 0.6f)
			return lerped
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

				val effect = AssetManager.loadParticleEffect("SpecialBeam")
				effect.colour = modifyColour(colour)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(dist * beamMoveSpeed, arrayOf(p1, p2), Interpolation.linear)
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
						grid.pop(cx, cy, 0f, special, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
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

				val effect = AssetManager.loadParticleEffect("SpecialBeam")
				effect.colour = modifyColour(colour)
				effect.killOnAnimComplete = true
				effect.animation = MoveAnimation.obtain().set(dist * beamMoveSpeed, arrayOf(p1, p2), Interpolation.linear)
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
						grid.pop(cx, cy, 0f, special, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
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

abstract class Match4(orb: Orb) : Special(orb)
{
	override fun merge(other: Orb): ((point: Point, grid: Grid, orb: Orb) -> Unit)?
	{
		if (other.special != null && other.special is Match4)
		{
			return fun (point: Point, grid: Grid, orb: Orb)
			{
				popColumn(this, orb.sprite.colour * other.sprite.colour, point.x, point.y, grid)
				popRow(this, orb.sprite.colour * other.sprite.colour, point.x, point.y, grid)
			}
		}

		return null
	}
}

class Horizontal4(orb: Orb) : Match4(orb)
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_vert", drawActualSize = true)
	}

	override fun copy(orb: Orb): Special = Horizontal4(orb)
	override fun apply() = fun (point: Point, grid: Grid, orb: Orb) {	popColumn(this, orb.sprite.colour, point.x, point.y, grid) }

}

class Vertical4(orb: Orb) : Match4(orb)
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_hori", drawActualSize = true)
	}

	override fun copy(orb: Orb): Special = Vertical4(orb)
	override fun apply() = fun (point: Point, grid: Grid, orb: Orb) {	popRow(this, orb.sprite.colour, point.x, point.y, grid) }
}

class DualMatch(orb: Orb) : Special(orb)
{
	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/orb_dual", drawActualSize = true)
	}

	override fun copy(orb: Orb): Special = DualMatch(orb)

	override fun merge(other: Orb): ((point: Point, grid: Grid, orb: Orb) -> Unit)?
	{
		val special = other.special
		if (special != null)
		{
			if (special is DualMatch)
			{
				return fun (point: Point, grid: Grid, orb: Orb)
				{
					val coreTile = grid.tile(point)

					val hitSet = ObjectSet<Tile>()

					val effect = AssetManager.loadParticleEffect("SpecialExplosion")
					effect.colour = modifyColour(orb.sprite.colour)
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
							grid.pop(cx, cy, 0f, this@DualMatch, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
						}
					}
					Future.call({
						for (tile in grid.grid)
						{
							if (!hitSet.contains(tile) && tile.dist(point) < 4)
							{
								hitSet.add(tile)
								grid.pop(tile.x, tile.y, 0f, this@DualMatch, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
							}
						}
								}, 0.1f)

					coreTile?.effects?.add(effect)
				}
			}
			else if (special is Horizontal4)
			{
				return fun (point: Point, grid: Grid, orb: Orb)
				{
					popColumn(this, orb.sprite.colour, point.x-1, point.y, grid)
					popColumn(this, orb.sprite.colour, point.x, point.y, grid)
					popColumn(this, orb.sprite.colour, point.x+1, point.y, grid)
				}
			}
			else if (special is Vertical4)
			{
				return fun (point: Point, grid: Grid, orb: Orb)
				{
					popRow(this, orb.sprite.colour, point.x, point.y-1, grid)
					popRow(this, orb.sprite.colour, point.x, point.y, grid)
					popRow(this, orb.sprite.colour, point.x, point.y+1, grid)
				}
			}
		}

		return null
	}

	override fun apply() = fun (point: Point, grid: Grid, orb: Orb)
	{
		val coreTile = grid.tile(point)

		val hitSet = ObjectSet<Tile>()

		val effect = AssetManager.loadParticleEffect("SpecialExplosion")
		effect.colour = modifyColour(orb.sprite.colour)
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
				grid.pop(cx, cy, 0f, this@DualMatch, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
			}
		}

		Future.call({
						for (tile in grid.grid)
						{
							if (!hitSet.contains(tile) && tile.dist(point) < 3)
							{
								hitSet.add(tile)
								grid.pop(tile.x, tile.y, 0f, this@DualMatch, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
							}
						}
					}, 0.1f)

		coreTile?.effects?.add(effect)
	}
}

class Match5(orb: Orb) : Special(orb)
{
	val flightTime = 0.3f

	init
	{
		sprite = AssetManager.loadSprite("Oryx/Custom/items/gem", drawActualSize = true)
		sprite.colourAnimation = ChromaticAnimation.obtain().set(15f)
	}

	override fun copy(orb: Orb): Special = Match5(orb)

	override fun merge(other: Orb): ((point: Point, grid: Grid, orb: Orb) -> Unit)?
	{
		val special = other.special
		if (special != null)
		{
			if (special is Match5)
			{
				return fun (point: Point, grid: Grid, orb: Orb)
				{
					for (tile in grid.grid)
					{
						if (tile.canHaveOrb)
						{
							val dst = tile.dist(point)
							val animDuration = 0.275f + dst * 0.05f

							val diff = tile.getPosDiff(point)
							diff[0].y *= -1

							val s = AssetManager.loadSprite("Oryx/Custom/items/shard")
							s.drawActualSize = false
							s.faceInMoveDirection = true
							s.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.5f)
							s.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.0f, false)
							s.completionCallback = fun()
							{
								grid.pop(tile, 0f, this, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 2)
							}
							tile.effects.add(s)
						}
					}
				}
			}
			else
			{
				val origSprite = other.sprite

				val key = other.key
				sprite.colourAnimation = null
				sprite.colour = other.sprite.colour
				other.sprite = sprite
				other.isChanger = false
				other.hasAttack = false

				return fun (point: Point, grid: Grid, orb: Orb)
				{
					val effect = AssetManager.loadParticleEffect("SpecialExplosion")
					effect.size[0] = 2
					effect.size[1] = 2
					effect.isCentered = true
					effect.colour = other.sprite.colour

					grid.tile(point)?.effects?.add(effect)

					for (tile in grid.grid)
					{
						if (tile.orb?.key == key)
						{
							val dst = tile.dist(point)
							val animDuration = 0.275f + dst * 0.05f

							val diff = tile.getPosDiff(point)
							diff[0].y *= -1

							val s = origSprite.copy()
							s.drawActualSize = false
							s.faceInMoveDirection = true
							s.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.5f)
							s.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.0f, false)
							s.completionCallback = fun()
							{
								if (tile.orb == null)
								{

								}
								else if (tile.orb!!.special == null)
								{
									tile.orb!!.special = special.copy(tile.orb!!)
								}
								else
								{
									val func = tile.orb!!.special!!.merge(other) ?: special.merge(tile.orb!!)
									tile.orb!!.armed = func
								}

								grid.pop(tile, 0f, this, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
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
								grid.pop(tile, 0f, this, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
							}
							tile.effects.add(s)
						}
					}
				}
			}
		}
		else
		{
			val key = other.key
			sprite.colourAnimation = null
			sprite.colour = other.sprite.colour
			other.sprite = sprite
			other.isChanger = false
			other.hasAttack = false

			return fun (point: Point, grid: Grid, orb: Orb)
			{
				val effect = AssetManager.loadParticleEffect("SpecialExplosion")
				effect.size[0] = 2
				effect.size[1] = 2
				effect.isCentered = true
				effect.colour = other.sprite.colour

				grid.tile(point)?.effects?.add(effect)

				for (tile in grid.grid)
				{
					if (tile.orb?.key == key || tile.creature != null || (tile.damageable?.maxhp ?: 0) > 1)
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
							grid.pop(tile, 0f, this, grid.level.player.getStat(Statistic.ABILITYDAMAGE) + grid.level.player.getStat(Statistic.MATCHDAMAGE) + 1)
						}
						tile.effects.add(s)
					}
				}
			}
		}
	}

	override fun apply(): (Point, Grid, orb: Orb) -> Unit
	{
		throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
	}
}
