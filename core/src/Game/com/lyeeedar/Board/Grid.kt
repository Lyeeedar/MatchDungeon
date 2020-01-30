package com.lyeeedar.Board

import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.kryo.deserialize
import com.kryo.serialize
import com.lyeeedar.Board.CompletionCondition.CompletionConditionCustomOrb
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Board.GridUpdate.*
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Game.Global
import com.lyeeedar.Game.GlobalDeck
import com.lyeeedar.Game.Player
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Statistic
import com.lyeeedar.UI.FullscreenMessage
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import com.lyeeedar.Util.Random
import com.migcomponents.migbase64.Base64
import ktx.collections.toGdxArray
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Grid(val width: Int, val height: Int, val level: Level, val replay: Replay)
{
	// ----------------------------------------------------------------------
	val ran = Random.obtainTS(replay.seed)

	// ----------------------------------------------------------------------
	val grid: Array2D<Tile> = Array2D(width, height ){ x, y -> Tile(x, y, this) }
	val spawnCount: Array2D<Int> = Array2D(width, height + 1){ _, _ -> 0 }

	// ----------------------------------------------------------------------
	val defaultAnimSpeed = 0.15f
	val animSpeedUpMultiplier = 0.975f
	var animSpeedMultiplier = 1f
	val animSpeed: Float
		get() = defaultAnimSpeed * animSpeedMultiplier

	var matchCount = 0

	// ----------------------------------------------------------------------
	val refillSprite = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.1f)

	// ----------------------------------------------------------------------
	var lastSwapped: Point = Point.MINUS_ONE

	// ----------------------------------------------------------------------
	val onTurn = Event0Arg()
	val onTime = Event1Arg<Float>()
	val onPop = Event2Arg<Entity, Float>()
	val onSunk = Event1Arg<Entity>()
	val onDamaged = Event1Arg<Entity>()
	val onSpawn = Event1Arg<Entity>()
	val onAttacked = Event1Arg<Entity>()

	// ----------------------------------------------------------------------
	val hitEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()

	// ----------------------------------------------------------------------
	var gainedBonusPower = false
	val poppedSpreaders = ObjectSet<String>()

	// ----------------------------------------------------------------------
	// Grid state
	val monsterTiles = Array<Tile>()
	val friendlyTiles = Array<Tile>()
	val sinkableTiles = Array<Tile>()
	val breakableTiles = Array<Tile>()
	val sinkPathTiles = Array<Tile>()
	val notSinkPathTiles = Array<Tile>()
	val basicOrbTiles = Array<Tile>()
	val attackTiles = Array<Tile>()
	val namedOrbTiles = Array<Tile>()

	// ----------------------------------------------------------------------
	var inTurn = false
	var isUpdating = false

	// ----------------------------------------------------------------------
	var noMatchTimer = 0f
	var matchHint: ValidMove? = null

	// ----------------------------------------------------------------------
	val noValidMoves: Boolean
		get() = matchHint == null && !inTurn && !isUpdating && activeAbility == null

	// ----------------------------------------------------------------------
	var dragStart: Point = Point.MINUS_ONE
	var toSwap: Pair<Point, Point>? = null

	// ----------------------------------------------------------------------
	val cascade = CascadeUpdateStep()
	val match = MatchUpdateStep()
	val sink = SinkUpdateStep()
	val detonate = DetonateUpdateStep()
	val updateGrid = UpdateGridUpdateStep()
	val updateGridState = UpdateGridStateUpdateStep()
	val cleanup = OnTurnCleanupUpdateStep()

	val updateSteps: kotlin.Array<AbstractUpdateStep>

	var hasAnim: Boolean = false

	// ----------------------------------------------------------------------
	val queuedActions = Array<DelayedAction>(false, 16)
	val delayedActions = ArrayDeque<DelayedAction>()

	// ----------------------------------------------------------------------
	var activeAbility: Ability? = null
		set(value)
		{
			if (level.isVictory || level.isDefeat) return

			field = value

			if (value == null)
			{
				for (tile in grid)
				{
					tile.isSelected = false
				}
			}
			else
			{
				dragStart = Point.MINUS_ONE

				if (value.targets == 0)
				{
					value.activate(this)
					inTurn = true
					field = null
				}
			}
		}

	// ----------------------------------------------------------------------
	init
	{
		updateSteps = arrayOf(
			cascade,
			match,
			sink,
			detonate,
			updateGridState,
			updateGrid,
			cleanup)

		onPop += fun (entity: Entity, delay: Float) : Boolean {

			val matchable = entity.matchable() ?: return false
			if (!matchable.skipPowerOrb)
			{
				if (Global.resolveInstantly)
				{
					PowerBar.instance.power++
					if (!gainedBonusPower)
					{
						gainedBonusPower = true

						val gain = level.player.getStat(Statistic.POWERGAIN).toInt()
						for (i in 0 until gain)
						{
							PowerBar.instance.power++
						}
					}
				}
				else
				{
					val pos = GridWidget.instance.pointToScreenspace(entity.pos().tile!!)
					val dst = PowerBar.instance.getOrbDest()
					val sprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/crystal_sky")

					if (dst != null)
					{
						Future.call({ spawnMote(pos, dst, sprite.copy(), 32f, {PowerBar.instance.power++}) }, delay)

						if (!gainedBonusPower)
						{
							gainedBonusPower = true

							val gain = level.player.getStat(Statistic.POWERGAIN).toInt()
							for (i in 0 until gain)
							{
								val dst = PowerBar.instance.getOrbDest() ?: break
								Future.call({ spawnMote(pos, dst, sprite.copy(), 32f, {PowerBar.instance.power++}) }, delay)
							}
						}
					}
				}
			}

			return false
		}
	}

	// ----------------------------------------------------------------------
	fun isUpdateBlocked(): Boolean
	{
		if (delayedActions.size > 0)
		{
			return true
		}
		else if (hasAnim)
		{
			return true
		}

		return false
	}

	// ----------------------------------------------------------------------
	fun refill()
	{
		replay.addMove(HistoryMove(true, grid.toString()))

		val tempgrid: Array2D<Tile> = Array2D(width, height ){ x, y -> Tile(x, y, this) }
		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				tempgrid[x, y].contents = grid[x, y].contents
			}
		}

		fill(true)

		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val tile = grid[x, y]
				val oldContents = tempgrid[x, y].contents

				if (oldContents?.isBasicOrb() != true)
				{
					tile.contents = oldContents
				}
				else
				{
					val newDesc = tile.contents!!.matchable()!!.desc
					tile.contents = oldContents

					val delay = tile.taxiDist(Point.ZERO).toFloat() * 0.1f

					tile.addDelayedAction(
						{ tile ->
							tile.contents!!.matchable()!!.setDesc(newDesc, tile.contents!!)

							if (!Global.resolveInstantly)
							{
								val sprite = refillSprite.copy()
								grid[x, y].effects.add(sprite)
							}
						}, delay + 0.2f)
				}
			}
		}

		inTurn = true
	}

	// ----------------------------------------------------------------------
	fun fill(orbOnly: Boolean)
	{
		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val tile = grid[x, y]
				if (tile.canHaveOrb && (tile.contents == null || tile.contents!!.matchable() != null))
				{
					val toSpawn = if (orbOnly) EntityArchetypeCreator.createOrb(OrbDesc.getRandomOrb(level)) else level.spawnOrb()

					val toSpawnMatchable = toSpawn.matchable()
					if (toSpawnMatchable != null)
					{
						val valid = OrbDesc.getValidOrbs(level)

						if (!orbOnly)
						{
							for (v in level.victoryConditions)
							{
								if (v is CompletionConditionCustomOrb)
								{
									if (ran.nextFloat() < v.orbChance)
									{
										valid.add(OrbDesc.getNamedOrb(v.targetOrbName))
									}
								}
							}
						}

						val l1 = tile(x - 1, y)?.contents?.matchable()?.desc
						val l2 = tile(x - 2, y)?.contents?.matchable()?.desc
						val u1 = tile(x, y - 1)?.contents?.matchable()?.desc
						val u2 = tile(x, y - 2)?.contents?.matchable()?.desc

						if (l1?.key == l2?.key)
						{
							valid.removeValue(l1, true)
						}
						if (u1?.key == u2?.key)
						{
							valid.removeValue(u1, true)
						}

						toSpawnMatchable.setDesc(valid.random(ran), toSpawn)
						if (toSpawnMatchable.isChanger) toSpawnMatchable.nextDesc = OrbDesc.getRandomOrb(level, toSpawnMatchable.desc)
					}

					grid[x, y].contents = toSpawn
				}
			}
		}
	}

	// ----------------------------------------------------------------------
	fun activateAbility()
	{
		if (level.isVictory || level.isDefeat) return

		replay.addMove(HistoryMove(Global.player.getAbilityIndex(activeAbility!!), activeAbility!!.selectedTargets.map{ it.copy() }.toGdxArray(), grid.toString()))

		activeAbility!!.activate(this)
		activeAbility = null

		inTurn = true
	}

	// ----------------------------------------------------------------------
	fun select(newSelection: Point)
	{
		if (isUpdateBlocked() || isUpdating || inTurn || level.isVictory || level.isDefeat) return

		if (activeAbility != null)
		{
			val newTile = tile(newSelection) ?: return
			if (!activeAbility!!.targetter.isValid(newTile, activeAbility!!.data)) return

			if (newTile.isSelected)
			{
				newTile.isSelected = false
				activeAbility!!.selectedTargets.removeValue(newTile, true)
			}
			else if (activeAbility!!.selectedTargets.size < activeAbility!!.targets)
			{
				newTile.isSelected = true
				activeAbility!!.selectedTargets.add(newTile)
			}
		}
		else
		{
			dragStart = newSelection
		}
	}

	// ----------------------------------------------------------------------
	fun dragEnd(selection: Point)
	{
		if (isUpdateBlocked() || isUpdating || inTurn || level.isVictory || level.isDefeat) return

		if (selection != dragStart && dragStart.dist(selection) == 1)
		{
			toSwap = Pair(dragStart, selection)
			dragStart = Point.MINUS_ONE
		}
	}

	// ----------------------------------------------------------------------
	fun clearDrag()
	{
		dragStart = Point.MINUS_ONE
	}

	// ----------------------------------------------------------------------
	fun update(deltaTime: Float)
	{
		val gridUpdateTrace = Statics.performanceTracer.getTrace("GridUpdate")
		gridUpdateTrace.start()

		for (step in updateSteps)
		{
			step.doUpdateRealTime(this, deltaTime)
		}

		if (!isUpdateBlocked())
		{
			for (step in updateSteps)
			{
				val completed = step.doUpdate(this)
				if (!completed)
				{
					isUpdating = true
					return
				}
			}

			if (inTurn)
			{
				for (step in updateSteps)
				{
					if (!step.wasRunThisTurn)
					{
						step.wasRunThisTurn = true
						step.doTurn(this)

						isUpdating = true
						return
					}
				}

				onTurn()

				for (step in updateSteps)
				{
					step.wasRunThisTurn = false
				}
			}
			else
			{
				if ((level.isVictory || level.isDefeat) && noMatchTimer > 0.5f)
				{
					level.complete()
				}
				else
				{
					updatePlayerInput(deltaTime)
				}
			}
			isUpdating = false
		}

		gridUpdateTrace.stop()
	}

	// ----------------------------------------------------------------------
	fun updatePlayerInput(delta: Float)
	{
		if (FullscreenMessage.instance == null)
		{
			if (activeAbility != null) noMatchTimer = 0f
			else noMatchTimer += delta

			// handle input
			if (toSwap != null)
			{
				val swapSuccess = swap()
				if (swapSuccess) inTurn = true
			}

			if (Tutorial.current == null && !noValidMoves) onTime(delta)
		}
	}

	// ----------------------------------------------------------------------
	private fun swap(): Boolean
	{
		val gridSnapshot = grid.toString()
		val historySwapStart = toSwap!!.first.copy()
		val historySwapEnd = toSwap!!.second.copy()

		val oldTile = tile(toSwap!!.first)
		val newTile = tile(toSwap!!.second)

		toSwap = null

		if (oldTile == null || newTile == null) return false

		if (oldTile.spreader != null || newTile.spreader != null) return false

		val oldEntity = oldTile.contents ?: return false
		val newEntity = newTile.contents ?: return false

		val oldSwap = oldEntity.swappable() ?: return false
		val newSwap = newEntity.swappable() ?: return false

		val oldSpecial = oldEntity.special()?.special
		val newSpecial = newEntity.special()?.special

		if (!oldSwap.canMove || !newSwap.canMove) return false

		oldSwap.cascadeCount = -1
		newSwap.cascadeCount = -1

		if (oldSpecial != null || newSpecial != null)
		{
			val merged = newSpecial?.merge(oldEntity) ?: oldSpecial?.merge(newEntity)
			if (merged != null)
			{
				addSpecial(newEntity, merged)

				if (!Global.resolveInstantly)
				{
					val sprite = oldEntity.renderable().renderable.copy()
					sprite.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(newTile.getPosDiff(oldTile, true)), Interpolation.linear)
					newTile.effects.add(sprite)
				}

				oldTile.contents = null

				merged.setArmed(true, newEntity)
				newEntity.markForDeletion(0f, "swap merged")

				lastSwapped = newTile
				matchHint = null

				replay.addMove(HistoryMove(historySwapStart, historySwapEnd, gridSnapshot))
				replay.logAction("Merging to form special $merged")
				return true
			}
		}

		oldTile.contents = newEntity
		newTile.contents = oldEntity

		oldEntity.pos().tile = newTile
		newEntity.pos().tile = oldTile

		val matches = match.findAllMatches(this)
		for (match in matches) match.free()
		if (matches.size == 0)
		{
			oldTile.contents = oldEntity
			newTile.contents = newEntity

			oldEntity.pos().tile = oldTile
			newEntity.pos().tile = newTile

			if (!Global.resolveInstantly)
			{
				var dir = Direction.getDirection(oldTile, newTile)
				if (dir.y != 0) dir = dir.opposite
				oldEntity.renderable().renderable.animation = BumpAnimation.obtain().set(animSpeed, dir)
			}

			if (Global.resolveInstantly)
			{
				throw RuntimeException("Swap was not valid!")
			}

			return false
		}
		else
		{
			lastSwapped = newTile
			matchHint = null

			if (!Global.resolveInstantly)
			{
				oldEntity.renderable().renderable.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(newTile.getPosDiff(oldTile)).invertY(), Interpolation.linear)
				newEntity.renderable().renderable.animation = MoveAnimation.obtain().set(animSpeed, UnsmoothedPath(oldTile.getPosDiff(newTile)).invertY(), Interpolation.linear)
			}

			replay.addMove(HistoryMove(historySwapStart, historySwapEnd, gridSnapshot))
			return true
		}
	}

	// ----------------------------------------------------------------------
	fun damage(tile: Tile, damageableEntity: Entity, delay: Float, damSource: Any? = null, bonusDam: Float = 0f, pierce: Float = 0f)
	{
		var logString = "damage (${tile.x},${tile.y}) with bonusDam($bonusDam) hitting entity(${damageableEntity.niceName()})"

		if (damageableEntity.isMarkedForDeletion())
		{
			logString += " but entity already marked for deletion"
			replay.logAction(logString)
			return
		}

		val damageable = damageableEntity.damageable()!!

		var targetDam = if (!damageable.damSources.contains(damSource)) 1f + bonusDam else 1f
		var damReduction = damageable.remainingReduction.toFloat()

		damReduction -= pierce
		if (damReduction < 0f)
		{
			damReduction = 0f
		}

		damReduction -= targetDam

		if (damReduction < 0)
		{
			targetDam = -damReduction
			damageable.remainingReduction = 0
		}
		else
		{
			targetDam = 0f
			damageable.remainingReduction = damReduction.ciel()

			logString += " blocked by DR"
		}

		logString += " dealing($targetDam) to hp(${damageable.hp})"

		damageable.hp -= targetDam
		if (damSource != null) damageable.damSources.add(damSource)
		onDamaged(damageableEntity)

		if (!Global.resolveInstantly)
		{
			val hit = hitEffect.copy()
			hit.renderDelay = delay
			tile.effects.add(hit)
		}

		if (damageable.hp <= 0)
		{
			damageableEntity.markForDeletion(delay, "killed")

			logString += " killing entity"
		}
		else
		{
			logString += " remaining hp(${damageable.hp})"
		}

		if (damageableEntity.isMonster())
		{
			val vampiric = Global.player.getStat(Statistic.VAMPIRICSTRIKES, withChoaticNature = true)
			val die = level.defeatConditions.filterIsInstance<CompletionConditionDie>().firstOrNull()
			if (die != null)
			{
				die.fractionalHp += targetDam * vampiric
			}
		}

		replay.logAction(logString)
	}

	// ----------------------------------------------------------------------
	fun pop(point: Point, delay: Float, damSource: Any? = null, bonusDam: Float = 0f, pierce: Float = 0f, skipPowerOrb: Boolean = false)
	{
		pop(point.x, point.y , delay, damSource, bonusDam, pierce, skipPowerOrb)
	}

	// ----------------------------------------------------------------------
	fun pop(x: Int, y: Int, delay: Float, damSource: Any? = null, bonusDam: Float = 0f, pierce: Float = 0f, skipPowerOrb: Boolean = false)
	{
		val tile = tile(x, y) ?: return

		if (tile.hasPlate)
		{
			tile.plateStrength--

			if (!Global.resolveInstantly)
			{
				val hit = hitEffect.copy()
				hit.renderDelay = delay
				tile.effects.add(hit)
			}
		}

		if (tile.spreader != null && damSource !is Spreader)
		{
			val spreader = tile.spreader!!
			poppedSpreaders.add(spreader.nameKey)
			tile.spreader = null

			if (!Global.resolveInstantly)
			{
				val hit = hitEffect.copy()
				hit.renderDelay = delay
				tile.effects.add(hit)
			}
		}

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

			return
		}
		else if (tile.contents?.damageable() != null)
		{
			damage(tile, tile.contents!!, delay, damSource, bonusDam, pierce)
			return
		}

		val contents = tile.contents
		val swappable = contents?.swappable() ?: return
		if (contents.isMarkedForDeletion()) return // already completed, dont do it again

		if (swappable.sealed)
		{
			swappable.sealCount--

			if (!Global.resolveInstantly)
			{
				val hit = hitEffect.copy()
				hit.renderDelay = delay
				tile.effects.add(hit)
			}

			return
		}

		if (contents.special() != null)
		{
			if (!swappable.sealed && contents.special()!!.special !is GemSpecial)
			{
				contents.special()!!.special.setArmed(true, contents)
			}
			else
			{
				return
			}
		}

		val matchable = contents.matchable()
		if (matchable != null)
		{
			contents.markForDeletion(delay, "popped")

			matchable.skipPowerOrb = skipPowerOrb

			if (!Global.resolveInstantly)
			{
				val sprite = matchable.desc.death.copy()
				sprite.colour = contents.renderable().renderable.colour
				sprite.renderDelay = delay
				sprite.isShortened = true

				tile.effects.add(sprite)
			}
		}

		if (contents.monsterEffect() != null)
		{
			contents.removeComponent(ComponentType.MonsterEffect)
		}
	}

	// ----------------------------------------------------------------------
	fun tile(point: Point): Tile? = tile(point.x, point.y)

	// ----------------------------------------------------------------------
	fun tile(x: Int, y:Int): Tile?
	{
		if (x >= 0 && y >= 0 && x < width && y < height) return grid[x, y]
		else return null
	}

	// ----------------------------------------------------------------------
	fun getTileClamped(point: Point) = getTile(MathUtils.clamp(point.x, 0, width-1), MathUtils.clamp(point.y, 0, height-1))!!

	// ----------------------------------------------------------------------
	fun getTile(point: Point) = getTile(point.x, point.y)

	// ----------------------------------------------------------------------
	fun getTile(point: Point, ox:Int, oy:Int) = getTile(point.x + ox, point.y + oy)

	// ----------------------------------------------------------------------
	fun getTile(point: Point, o: Point) = getTile(point.x + o.x, point.y + o.y)

	// ----------------------------------------------------------------------
	fun getTile(x: Int, y: Int, dir: Direction) = getTile(x + dir.x, y + dir.y)

	// ----------------------------------------------------------------------
	fun getTile(point: Point, dir: Direction) = getTile(point.x + dir.x, point.y + dir.y)

	// ----------------------------------------------------------------------
	fun getTile(x: Int, y: Int): Tile? = grid[x, y, null]

	// ----------------------------------------------------------------------
	fun dispose()
	{
		for (tile in grid)
		{
			tile.contents?.free()
		}

		EntityPool.flushFreedEntities()
	}
}

class HistoryMove()
{
	var swapStart: Point? = null
	var swapEnd: Point? = null
	var refill = false
	var abilityIndex: Int? = null
	var abilityTargets: Array<Point>? = null
	var levelSnapshot: String = ""

	val gridActionLog = Array<String>()

	constructor(swapStart: Point, swapEnd: Point, levelSnapshot: String) : this()
	{
		this.swapStart = swapStart
		this.swapEnd = swapEnd
		this.levelSnapshot = levelSnapshot
	}

	constructor(refill: Boolean, levelSnapshot: String) : this()
	{
		this.refill = refill
		this.levelSnapshot = levelSnapshot
	}

	constructor(abilityIndex: Int, targets: Array<Point>, levelSnapshot: String) : this()
	{
		this.abilityIndex = abilityIndex
		this.abilityTargets = targets
		this.levelSnapshot = levelSnapshot
	}

	override fun toString(): String
	{
		if (swapStart != null)
		{
			return "swapping (${swapStart!!.toShortString()}) with (${swapEnd!!.toShortString()})"
		}
		else if (refill)
		{
			return "refilling grid"
		}
		else if (abilityIndex != null)
		{
			return "using ability $abilityIndex on " + abilityTargets!!.joinToString(" ") { "(${it.toShortString()})" }
		}
		else
		{
			return "Unknown move"
		}
	}
}

class Replay()
{
	lateinit var questTheme: String
	lateinit var levelPath: String
	var variant: Int = -1
	var seed: Long = -1
	lateinit var globalDeck: ByteArray
	lateinit var player: ByteArray
	lateinit var globalflags: ByteArray
	lateinit var questflags: ByteArray
	lateinit var cardflags: ByteArray

	val moves = Array<HistoryMove>()

	constructor(questTheme: String, levelPath: String, variant: Int, seed: Long, player: Player, deck: GlobalDeck) : this()
	{
		this.questTheme = questTheme
		this.levelPath = levelPath
		this.variant = variant
		this.seed = seed
		this.globalDeck = serialize(deck)
		this.player = serialize(player)
		this.globalflags = serialize(Global.globalflags)
		this.questflags = serialize(Global.questflags)
		this.cardflags = serialize(Global.cardflags)
	}

	fun logAction(action: String)
	{
		Statics.logger.logDebug("Logging action: $action")

		moves.last().gridActionLog.add(action)

		uploadCrashData()
	}

	fun addMove(move: HistoryMove)
	{
		Statics.logger.logDebug("Making move: $move")

		moves.add(move)

		uploadCrashData()
	}

	fun compress(): ByteArray
	{
		val rawBytes = serialize(this)

		val outStream = ByteArrayOutputStream(rawBytes.size)
		val compressionStream = GZIPOutputStream(outStream)

		compressionStream.write(rawBytes)
		compressionStream.close()
		outStream.close()

		val compressedBytes = outStream.toByteArray()

		return compressedBytes
	}

	fun compressToString(): String
	{
		val bytes = compress()
		val asString = Base64.encodeToString(bytes, false)

		return asString
	}

	override fun toString(): String
	{
		val builder = StringBuilder()
		for (i in 0 until moves.size)
		{
			val move = moves[i]

			builder.appendln("Move $i:")
			builder.appendln(move.levelSnapshot)
			builder.appendln("\n")
			builder.appendln(move.toString())
			for (item in move.gridActionLog)
			{
				builder.appendln(item)
			}
			builder.appendln("\n")
		}

		return builder.toString()
	}

	fun uploadCrashData()
	{
		val asString = compressToString()
		val chunked = asString.chunked(1023)

		Statics.crashReporter.setCustomKey("CrashDataNumParts", chunked.size)
		for (i in chunked.indices)
		{
			Statics.crashReporter.setCustomKey("CrashDataPart$i", chunked[i])
		}
	}

	companion object
	{
		fun loadFromBytes(compressedBytes: ByteArray): Replay
		{
			val inputByteWriter = ByteArrayInputStream(compressedBytes)
			val decompressionStream = GZIPInputStream(inputByteWriter)
			val rawBytes = decompressionStream.readBytes()
			val replay = deserialize(rawBytes) as Replay

			return replay
		}

		fun loadFromString(string: String): Replay
		{
			val bytes = Base64.decode(string)
			return loadFromBytes(bytes)
		}
	}
}