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
import com.lyeeedar.Game.*
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.lambda
import com.lyeeedar.UI.shake
import com.lyeeedar.Util.*
import ktx.actors.then
import ktx.collections.toGdxArray

class MatchUpdateStep : AbstractUpdateStep()
{
	// ----------------------------------------------------------------------
	private fun match(grid: Grid): Boolean
	{
		val trace = Statics.crashReporter.getTrace("Match")
		trace.start()

		val matches = findMatches(grid.grid, 3)
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

		trace.stop()

		return matches.size == 0
	}

	// ----------------------------------------------------------------------
	fun findMatches(grid: Grid) : Array<Match>
	{
		val matches = Array<Match>(false, 16)

		matches.addAll(findMatches(grid.grid, 3))
		matches.addAll(findMatches(grid.grid, 4))
		matches.addAll(findMatches(grid.grid, 5))

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
	fun findMatches(grid: Array2D<Tile>, length: Int, exact: Boolean = false) : Array<Match>
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
				val tile = grid[x, y]
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
				val tile = grid[x, y]
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

		val seenThisMatch = ObjectSet<Entity>()

		// remove all orbs, activate all specials
		for (match in matches)
		{
			coreTiles.clear()
			borderTiles.clear()

			for (point in match.points())
			{
				val tile = grid.grid[point]
				coreTiles.add(tile)

				if (Global.resolveInstantly && tile.contents?.isMarkedForDeletion() == true && !seenThisMatch.contains(tile.contents) && tile.contents?.special() == null)
				{
					throw RuntimeException("Trying to match a deleted object")
				}
				if (tile.contents != null)
				{
					seenThisMatch.add(tile.contents)
				}

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
				handleMatch(grid, t)
			}
		}


		// figure out if we should spawn specials due to 4 / 5 / cross match
		fun animateMerge(tile: Tile, point: Point, sprite: Renderable)
		{
			if (!Global.resolveInstantly)
			{
				val sprite = sprite.copy()

				if (sprite is Sprite)
				{
					sprite.drawActualSize = false
				}

				sprite.animation = MoveAnimation.obtain().set(grid.animSpeed, UnsmoothedPath(tile.getPosDiff(point)).invertY(), Interpolation.linear)

				tile.effects.add(sprite)
			}
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

				val special = getSpecial(grid, tile.associatedMatches[0]!!.length(), tile.associatedMatches[1]!!.length(), Direction.CENTER) ?: continue
				val merged = tryMergeSpecial(special, contents)

				grid.replay.logAction("Spawning special $merged at (${tile.toShortString()})")

				val orb = EntityArchetypeCreator.createOrb(contents.matchable()!!.desc)
				addSpecial(orb, merged)
				orb.pos().setTile(orb, tile)

				tile.associatedMatches[0]!!.used = true
				tile.associatedMatches[1]!!.used = true

				for (point in tile.associatedMatches[0]!!.points())
				{
					animateMerge(tile, point, sprite)
				}

				for (point in tile.associatedMatches[1]!!.points())
				{
					animateMerge(tile, point, sprite)
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

					val special = getSpecial(grid, match.length(), 0, match.direction()) ?: continue
					val merged = tryMergeSpecial(special, contents)

					grid.replay.logAction("Spawning special $merged at (${tile.toShortString()})")

					val orb = EntityArchetypeCreator.createOrb(contents.matchable()!!.desc)
					addSpecial(orb, merged)
					orb.pos().setTile(orb, tile)

					for (point in match.points())
					{
						animateMerge(tile, point, sprite)
					}
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun handleMatch(grid: Grid, tile: Tile)
	{
		if (tile.contents?.healable() != null)
		{
			val friendly = tile.contents!!.healable()!!
			if (friendly.hp < friendly.maxhp)
			{
				friendly.hp++

				if (!Global.resolveInstantly)
				{
					val healSprite = AssetManager.loadParticleEffect("Heal").getParticleEffect()
					healSprite.colour = Colour.GREEN
					tile.effects.add(healSprite)
				}
			}
		}
		else if (tile.contents?.damageable() != null)
		{
			grid.damage(tile, tile.contents!!, 0f, this, grid.level.player.getStat(Statistic.MATCHDAMAGE), grid.level.player.getStat(Statistic.PIERCE))
		}

		if (tile.spreader != null)
		{
			val spreader = tile.spreader!!
			tile.spreader = null
			grid.poppedSpreaders.add(spreader.nameKey)

			if (!Global.resolveInstantly)
			{
				tile.effects.add(grid.hitEffect.copy())
			}
		}
	}

	// ----------------------------------------------------------------------
	private fun getSpecial(grid: Grid, count1: Int, count2: Int, dir: Direction): Special?
	{
		if (count1 >= 5 || count2 >= 5)
		{
			return Match5()
		}
		else if (count1 > 0 && count2 > 0)
		{
			return DualMatch()
		}
		else if (dir.y != 0 && count1 == 4)
		{
			return Vertical4()
		}
		else if (dir.x != 0 && count1 == 4)
		{
			return Horizontal4()
		}

		return null
	}

	// ----------------------------------------------------------------------
	fun displayMatchMessage(grid: Grid, point: Point)
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
	override fun doUpdate(grid: Grid): Boolean
	{
		return match(grid)
	}

	// ----------------------------------------------------------------------
	override fun doTurn(grid: Grid)
	{

	}

	// ----------------------------------------------------------------------
	override fun doUpdateRealTime(grid: Grid, deltaTime: Float)
	{

	}

	// ----------------------------------------------------------------------
	companion object
	{
		fun test()
		{
			doTest(
				arrayOf(
					charArrayOf('A', 'A', 'A', 'B', 'C'),
					charArrayOf('C', 'C', 'F', 'H', 'C'),
					charArrayOf('R', 'R', 'S', 'D', 'C'),
					charArrayOf('Y', 'R', 'P', 'Q', 'S'),
					charArrayOf('A', 'G', 'H', 'D', 'T')
					   ),
				arrayOf(
					arrayOf(Point(0, 0), Point(1, 0), Point(2, 0)),
					arrayOf(Point(4, 0), Point(4, 1), Point(4, 2)),
					arrayOf(Point(0, 1), Point(1, 1)),
					arrayOf(Point(0, 2), Point(1, 2)),
					arrayOf(Point(1, 2), Point(1, 3))
					   )
				  )
		}

		private fun doTest(gridTemplate: kotlin.Array<CharArray>, expectedMatches: kotlin.Array<kotlin.Array<Point>>)
		{
			val width = gridTemplate[0].size
			val height = gridTemplate.size

			val deck = GlobalDeck()
			deck.chosenCharacter = Character("")
			val player = Player(deck.chosenCharacter, PlayerDeck())

			val tempGrid = Grid(width, height, Level(""), Replay("", "", 0, 1, player, deck))
			val grid = Array2D<Tile>(width, height) { x,y -> Tile(x, y, tempGrid) }

			for (x in 0 until grid.width)
			{
				for (y in 0 until grid.height)
				{
					val tile = grid[x, y]

					val desc = OrbDesc()
					desc.key = gridTemplate[y][x].hashCode()

					tile.contents = EntityArchetypeCreator.createOrb(desc)
				}
			}

			val matcher = MatchUpdateStep()
			val matches = matcher.findMatches(grid, 2)

			if (matches.size != expectedMatches.size)
			{
				throw RuntimeException("Didnt get the expected number of matches!")
			}

			for (expected in expectedMatches)
			{
				var found = false
				for (match in matches)
				{
					val matchPoints = match.points().toGdxArray()
					if (matchPoints.size == expected.size && expected.all { matchPoints.contains(it) })
					{
						matches.removeValue(match, true)
						found = true
						break
					}
				}

				if (!found)
				{
					throw java.lang.RuntimeException("Failed to find match in returned matches!")
				}
			}
		}
	}
}

data class Match(val p1: Point, val p2: Point, var used: Boolean = false)
{
	fun length() = p1.dist(p2) + 1
	fun points() = p1.rangeTo(p2)
	fun direction() = Direction.getDirection(p1, p2)
	fun free()
	{
		p1.free()
		p2.free()
	}
}