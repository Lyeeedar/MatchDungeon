package com.lyeeedar.Board.GridUpdate

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.*
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.Systems.GridSystem
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.lambda
import com.lyeeedar.UI.shake
import com.lyeeedar.Util.*
import ktx.actors.then

class MatchUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	private fun match(grid: Grid): Boolean
	{
		val matches = findMatches(grid, 3)
		clearMatches(grid, matches)
		for (match in matches) match.free()

		grid.lastSwapped = Point.MINUS_ONE

		if (matches.size > 0)
		{
			grid.matchCount++
			grid.animSpeedMultiplier *= grid.animSpeedUpMultiplier

			val chosen = matches.random()
			val point = chosen.points().asSequence().random()
			displayMatchMessage(grid, point!!)
		}

		return matches.size == 0
	}

	// ----------------------------------------------------------------------
	private fun findMatches(grid: Grid) : Array<Match>
	{
		val matches = Array<Match>(false, 16)

		matches.addAll(findMatches(grid, 3))
		matches.addAll(findMatches(grid, 4))
		matches.addAll(findMatches(grid, 5))

		// clear duplicates
		var i = 0
		while (i < matches.size)
		{
			val pair = matches[i]

			var ii = i+1
			while (ii < matches.size)
			{
				val opair = matches[ii]

				if (opair == pair)
				{
					matches.removeIndex(ii)
				}
				else
				{
					ii++
				}
			}

			i++
		}

		return matches
	}

	// ----------------------------------------------------------------------
	private fun findMatches(grid: Grid, length: Int, exact: Boolean = false) : Array<Match>
	{
		val matches = Array<Match>()

		fun addMatch(p1: Point, p2: Point)
		{
			fun check(dst: Int): Boolean
			{
				if (exact) return dst == length-1
				else return dst >= length-1
			}

			val dst = p1.dist(p2)
			if (check(dst))
			{
				// check not already added
				matches.add(Match(p1, p2))
			}
		}

		// Match rows
		for (y in 0 until grid.height)
		{
			var sx = -1
			var key = -1

			for (x in 0 until grid.width)
			{
				val tile = grid.grid[x, y]
				val matchable = tile.contents?.matchable()

				if (matchable == null || tile.spreader != null)
				{
					if (key != -1)
					{
						addMatch(Point.obtain().set(sx,y), Point.obtain().set(x-1,y))
					}

					key = -1
				}
				else
				{
					if (matchable.desc.key != key || !matchable.canMatch)
					{
						// if we were matching, close matching
						if (key != -1)
						{
							addMatch(Point.obtain().set(sx,y), Point.obtain().set(x-1,y))
						}

						sx = x
						key = matchable.desc.key
					}
				}
			}

			if (key != -1)
			{
				addMatch(Point.obtain().set(sx,y), Point.obtain().set(grid.width-1,y))
			}
		}

		// Match columns
		for (x in 0 until grid.width)
		{
			var sy = -1
			var key = -1

			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]
				val matchable = tile.contents?.matchable()

				if (matchable == null || tile.spreader != null)
				{
					if (key != -1)
					{
						addMatch(Point.obtain().set(x,sy), Point.obtain().set(x,y-1))
					}

					key = -1
				}
				else
				{
					if (matchable.desc.key != key || !matchable.canMatch)
					{
						// if we were matching, close matching
						if (key != -1)
						{
							addMatch(Point.obtain().set(x,sy), Point.obtain().set(x,y-1))
						}

						sy = y
						key = matchable.desc.key
					}
				}
			}

			if (key != -1)
			{
				addMatch(Point.obtain().set(x,sy), Point.obtain().set(x,grid.height-1))
			}
		}

		return matches
	}

	// ----------------------------------------------------------------------
	private fun clearMatches(grid: Grid, matches: Array<Match>)
	{
		// mark all matched tiles with the matches associated with them
		for (x in 0 until grid.width) for (y in 0 until grid.height)
		{
			val tile = grid.grid[x, y]

			tile.associatedMatches[0] = null
			tile.associatedMatches[1] = null
		}

		for (match in matches)
		{
			for (point in match.points())
			{
				val tile = grid.grid[point]
				if (tile.associatedMatches[0] == null)
				{
					tile.associatedMatches[0] = match
				}
				else
				{
					tile.associatedMatches[1] = match
				}
			}
		}

		val coreTiles = Array<Tile>()
		val borderTiles = ObjectSet<Tile>()

		// remove all orbs, activate all specials
		for (match in matches)
		{
			coreTiles.clear()
			borderTiles.clear()

			for (point in match.points())
			{
				coreTiles.add(grid.grid[point])
				grid.pop(point.x, point.y, 0f)
			}

			for (tile in coreTiles)
			{
				for (d in Direction.CardinalValues)
				{
					val t = grid.tile(tile.x + d.x, tile.y + d.y) ?: continue
					if (!coreTiles.contains(t, true))
					{
						borderTiles.add(t)
					}
				}
			}

			// pop all borders
			for (t in borderTiles)
			{
				handleMatch(t)
			}
		}

		fun clearMatchPoint(tile: Tile, point: Point, sprite: Renderable)
		{
			val sprite = sprite.copy()

			if (sprite is Sprite)
			{
				sprite.drawActualSize = false
			}

			if (!Global.resolveInstantly)
			{
				sprite.animation = MoveAnimation.obtain().set(grid.animSpeed, UnsmoothedPath(tile.getPosDiff(point)).invertY(), Interpolation.linear)
			}

			tile.effects.add(sprite)
		}

		// for each tile with 2 matches spawn the relevant special, and mark the matches as used, if cross point is used then spawn in a neighbouring tile that isnt specialed
		for (x in 0 until grid.width) for (y in 0 until grid.height)
		{
			val tile = grid.grid[x, y]

			val contents = tile.contents
			val matchable = contents?.matchable()

			if (tile.associatedMatches[0] != null && tile.associatedMatches[1] != null && !matchable!!.desc.isNamed)
			{
				val sprite = contents.renderable().renderable

				val special = getSpecial(grid, tile.associatedMatches[0]!!.length(), tile.associatedMatches[1]!!.length(), Direction.CENTER, matchable.desc) ?: continue

				spawnSpecial(contents, special)

				tile.associatedMatches[0]!!.used = true
				tile.associatedMatches[1]!!.used = true

				for (point in tile.associatedMatches[0]!!.points())
				{
					clearMatchPoint(tile, point, sprite)
				}

				for (point in tile.associatedMatches[1]!!.points())
				{
					clearMatchPoint(tile, point, sprite)
				}
			}
		}

		// for each unused match spawn the relevant special at the player swap pos, else at the center, else at a random unspecialed tile
		for (match in matches)
		{
			if (!match.used && match.length() > 3)
			{
				val tile = grid.grid[match.points().maxBy { grid.grid[it].contents?.swappable()?.cascadeCount ?: 0 }!!]

				val contents = tile.contents
				val matchable = contents?.matchable()!!
				if (!matchable.desc.isNamed)
				{
					val sprite = contents.renderable().renderable

					val special = getSpecial(grid, match.length(), 0, match.direction(), matchable.desc) ?: continue

					spawnSpecial(contents, special)

					for (point in match.points())
					{
						clearMatchPoint(tile, point, sprite)
					}
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun handleMatch(tile: Tile)
	{
		if (t.friendly != null)
		{
			val friendly = t.friendly!!
			if (friendly.hp < friendly.maxhp)
			{
				t.friendly!!.hp++
				val healSprite = AssetManager.loadParticleEffect("Heal").getParticleEffect()
				healSprite.colour = Colour.GREEN
				t.effects.add(healSprite)
			}
		}
		else if (t.damageable != null)
		{
			damage(t, t.damageable!!, 0f, this, level.player.getStat(Statistic.MATCHDAMAGE), level.player.getStat(Statistic.PIERCE))
		}

		if (t.spreader != null)
		{
			val spreader = t.spreader!!

			t.spreader = null

			poppedSpreaders.add(spreader.nameKey)

			t.effects.add(hitEffect.copy())
		}
	}

	// ----------------------------------------------------------------------
	private fun spawnSpecial(contents: Entity, special: Special)
	{
		val specialComponent = contents.special()
		if (specialComponent != null)
		{
			specialComponent.special = specialComponent.special.merge(special) ?: special.merge(specialComponent.special) ?: special
			specialComponent.special.armed = true

			contents.add(MarkedForDeletionComponent.obtain())
		}
		else
		{
			contents.add(SpecialComponent.obtain().set(special))
		}
	}

	// ----------------------------------------------------------------------
	private fun getSpecial(grid: Grid, count1: Int, count2: Int, dir: Direction, desc: OrbDesc): Special?
	{
		if (count1 >= 5 || count2 >= 5)
		{
			return Match5(desc, grid.level.theme)
		}
		else if (count1 > 0 && count2 > 0)
		{
			return DualMatch(desc, grid.level.theme)
		}
		else if (dir.y != 0 && count1 == 4)
		{
			return Vertical4(desc, grid.level.theme)
		}
		else if (dir.x != 0 && count1 == 4)
		{
			return Horizontal4(desc, grid.level.theme)
		}

		return null
	}

	// ----------------------------------------------------------------------
	private fun displayMatchMessage(grid: Grid, point: Point)
	{
		if (Global.resolveInstantly) return

		data class MessageData(val text: String, val colour: Colour, val size: Float)
		val message = when(grid.matchCount)
		{
			4 -> MessageData("Impressive", Colour(0.8f, 0.9f, 1f, 1f), 1f)
			7 -> MessageData("Amazing", Colour(0.8f, 1f, 0.9f, 1f), 1.3f)
			10 -> MessageData("Spectacular", Colour(1f, 0.8f, 0.9f, 1f), 1.6f)
			14 -> MessageData("Magical", Colour(0.2f, 0.82f, 1f, 1f), 1.9f)
			18 -> MessageData("Legendary", Colour(1f, 0.81f, 0.5f, 1f), 2.2f)
			22 -> MessageData("Mythical", Colour(0.8f, 0.5f, 0.95f, 1f), 2.5f)
			26 -> MessageData("Divine", Colour(0.95f, 1f, 0.81f, 1f), 2.8f)
			30 -> MessageData("Godlike", Colour(0.8f, 0.55f, 0.78f, 1f), 3.1f)
			else -> null
		}
		val maxVal = 32f

		if (message != null)
		{
			val pos = GridWidget.instance.pointToScreenspace(point)

			val label = Label(message.text, Statics.skin, "popup")
			label.color = message.colour.color()
			label.setFontScale(message.size)
			label.rotation = -60f
			label.setPosition(pos.x, pos.y)

			val sequence =
				Actions.alpha(0f) then
					Actions.fadeIn(0.1f) then
					Actions.parallel(Actions.moveBy(MathUtils.random(-2f, 2f), MathUtils.random(0f, 2f), 1f), shake(grid.matchCount.toFloat() / maxVal, 0.03f, 1f)) then
					Actions.fadeOut(0.1f) then
					lambda { messageList.removeValue(label, true) } then
					Actions.removeActor()

			label.addAction(sequence)

			val width = label.prefWidth
			if (pos.x + width > Statics.stage.width)
			{
				label.setPosition(Statics.stage.width - width - 20, pos.y)
			}

			messageList.add(label)
			Statics.stage.addActor(label)
		}
	}
	val messageList = Array<Label>()

	// ----------------------------------------------------------------------
	override fun doUpdate(gridSystem: GridSystem): Boolean
	{
		return match(gridSystem.grid!!)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(gridSystem: GridSystem)
	{

	}
}