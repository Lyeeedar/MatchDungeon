package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.lyeeedar.Board.CompletionCondition.AbstractCompletionCondition
import com.lyeeedar.Board.CompletionCondition.CompletionConditionCustomOrb
import com.lyeeedar.Board.CompletionCondition.CompletionConditionSink
import com.lyeeedar.Game.Player
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.SpriteWrapper
import com.lyeeedar.Util.*
import ktx.collections.set
import ktx.collections.toGdxArray

/**
 * Created by Philip on 13-Jul-16.
 */

class Level(val loadPath: String)
{
	// Load Data
	lateinit var charGrid: Array2D<Char>

	val symbolMap = IntMap<Symbol>()

	val defeatConditions = Array<AbstractCompletionCondition>()
	val victoryConditions = Array<AbstractCompletionCondition>()

	var orbs: Int = 6

	var sealStrength = 1
	var blockStrength = 1
	var plateStrength = 1

	val factions = Array<String>()

	var customMonster: MonsterDesc? = null

	// Active state data
	lateinit var theme: Theme
	lateinit var grid: Grid
	lateinit var player: Player
	var completed = false
	var completeFun: (() -> Unit)? = null
	val onComplete = Event0Arg()

	lateinit var victoryAction: () -> Unit
	lateinit var defeatAction: () -> Unit

	fun spawnOrb(): Swappable
	{
		for (v in victoryConditions)
		{
			if (v is CompletionConditionCustomOrb)
			{
				if (Random.random.nextFloat() < v.orbChance)
				{
					return Orb(Orb.getNamedOrb(v.targetOrbName), theme)
				}
			}
		}

		val toSpawn = theme.spawnList.random()

		if (toSpawn == "Changer")
		{
			val orb = Orb(Orb.getRandomOrb(this), theme)
			orb.isChanger = true
			return orb
		}
		else if (toSpawn == "Attack")
		{
			val orb = Orb(Orb.getRandomOrb(this), theme)
			orb.hasAttack = true
			orb.attackTimer = 10
			return orb
		}

		return Orb(Orb.getRandomOrb(this), theme)
	}

	fun create(theme: Theme, player: Player, victoryAction: () -> Unit, defeatAction: () -> Unit)
	{
		this.theme = theme
		this.player = player
		this.victoryAction = victoryAction
		this.defeatAction = defeatAction

		grid = Grid(charGrid.xSize, charGrid.ySize, this)

		var hasMonster = false

		fun loadTile(tile: Tile, char: Char)
		{
			if (char == '#')
			{
				tile.canHaveOrb = false
				tile.spriteSetter = theme.floor.copy()
				tile.spriteSetter = theme.wall.copy()
			}
			else if (char == '~')
			{
				tile.canHaveOrb = false
				tile.isPit = true
				tile.spriteSetter = theme.pit.copy()
			}
			else if (char == 'p')
			{
				tile.canHaveOrb = true
				tile.spriteSetter = theme.floor.copy()
				tile.plateStrength = plateStrength
			}
			else if (char == '=')
			{
				tile.canHaveOrb = true
				tile.spriteSetter = theme.floor.copy()
				tile.block = Block(theme)
				tile.block!!.maxhp = blockStrength
			}
			else if (char == '$')
			{
				tile.chest = Chest(true, theme)
				tile.canHaveOrb = false
				tile.spriteSetter = theme.floor.copy()
				tile.chest!!.attachHandlers(grid)
			}
			else if (char == '£')
			{
				tile.chest = Chest(false, theme)
				tile.canHaveOrb = false
				tile.spriteSetter = theme.floor.copy()
				tile.chest!!.attachHandlers(grid)
			}
			else if (char == '!')
			{
				tile.canHaveOrb = true
				tile.spriteSetter = theme.floor.copy()

				hasMonster = true
			}
			else if (symbolMap.containsKey(char.toInt()))
			{
				val symbol = symbolMap[char.toInt()]
				if (symbol.extends != ' ')
				{
					loadTile(tile, symbol.extends)
				}
				else
				{
					loadTile(tile, '.')
				}

				if (symbol.sprite != null)
				{
					tile.spriteSetter = symbol.sprite.copy()
				}

				if (symbol.block != null)
				{
					tile.block = Block(theme)

					if (symbol.block.sprite != null)
					{
						tile.block!!.sprite = symbol.block.sprite.copy()
					}

					tile.block!!.maxhp = symbol.block.hp
					tile.block!!.alwaysShowHP = symbol.block.alwaysShowHP
				}

				tile.plateStrength = symbol.plate

				if (symbol.isMonster)
				{
					hasMonster = true
				}

				if (symbol.type == SymbolType.PIT)
				{
					tile.canHaveOrb = false
					tile.isPit = true
				}
				else if (symbol.type == SymbolType.WALL)
				{
					tile.canHaveOrb = false
					tile.isPit = false
				}
				else
				{
					tile.canHaveOrb = true
					tile.isPit = false
				}

				if (symbol.isChest)
				{
					tile.chest = Chest(true, theme)
					tile.spriteSetter = theme.floor.copy()
					tile.chest!!.attachHandlers(grid)

					tile.canHaveOrb = false
					tile.isPit = false
				}

				if (symbol.spreader != null)
				{
					tile.spreader = symbol.spreader.copy()
				}
			}
			else
			{
				tile.spriteSetter = theme.floor.copy()
			}
		}

		for (x in 0 until charGrid.xSize)
		{
			for (y in 0 until charGrid.ySize)
			{
				val tile = grid.grid[x, y]
				val char = charGrid[x, y]

				loadTile(tile, char)
			}
		}

		if (hasMonster)
		{
			val chosenFactionName = factions.random()
			var chosenFaction: Faction? = null
			if (chosenFactionName.isBlank())
			{
				if (customMonster != null)
				{
					chosenFaction = Faction.createCustomFaction(this)
				}
			}
			else
			{
				chosenFaction = Faction.load(chosenFactionName)
			}

			if (chosenFaction == null)
			{
				chosenFaction = Faction.load(theme.factions.random())
			}

			// iterate through and find groups
			data class MonsterMarker(val monsterDesc: MonsterDesc?, val isBoss: Boolean, var used: Boolean = false)
			val monsterGrid = Array2D<MonsterMarker?>(charGrid.xSize, charGrid.ySize)

			fun loadMonster(tile: Tile, char: Char): MonsterMarker?
			{
				var monster: MonsterMarker? = null

				if (char == '!')
				{
					monster = MonsterMarker(null, false)
				}
				else if (char == '?')
				{
					monster = MonsterMarker(null, true)
				}
				else if (symbolMap.containsKey(char.toInt()))
				{
					val symbol = symbolMap[char.toInt()]
					if (symbol.extends != ' ')
					{
						monster = loadMonster(tile, symbol.extends)
					}

					if (symbol.isMonster)
					{
						monster = MonsterMarker(symbol.monsterDesc, false)
					}
				}

				return monster
			}

			for (x in 0 until charGrid.xSize)
			{
				for (y in 0 until charGrid.ySize)
				{
					val tile = grid.grid[x, y]
					monsterGrid[x, y] = loadMonster(tile, charGrid[x, y])
				}
			}

			for (x in 0 until charGrid.xSize)
			{
				for (y in 0 until charGrid.ySize)
				{
					val monsterMarker = monsterGrid[x, y]
					if (monsterMarker != null && !monsterMarker.used)
					{
						// Try sizes until we fail
						var size = 1
						outer@ while (true)
						{
							for (ix in 0 until size+1)
							{
								for (iy in 0 until size+1)
								{
									if (!monsterGrid.inBounds(x+ix, y+iy))
									{
										break@outer
									}

									val otherMonster = monsterGrid[x+ix, y+iy]
									if (otherMonster == null || otherMonster.used || otherMonster.monsterDesc != monsterMarker.monsterDesc || otherMonster.isBoss != monsterMarker.isBoss)
									{
										break@outer
									}
								}
							}

							size++
						}

						// Spawn monster for found size
						val monsterDesc = monsterMarker.monsterDesc ?: if (monsterMarker.isBoss) chosenFaction.getBoss(size) else chosenFaction.get(size)
						val monster = Monster(monsterDesc)
						monster.size = size

						for (ix in 0 until size)
						{
							for (iy in 0 until size)
							{
								val gx = x + ix
								val gy = y + iy

								val tile = grid.grid[gx, gy]

								tile.monster = monster
								monster.tiles[ix, iy] = tile

								monsterGrid[gx, gy]!!.used = true
							}
						}
					}
				}
			}
		}

		grid.fill(false)

		fun modifyOrbs(tile: Tile, char: Char)
		{
			val swappable = tile.swappable

			if (char == '|')
			{
				var orb = swappable as? Orb
				if (orb == null) orb = Orb(Orb.getRandomOrb(this), theme)
				tile.special = Horizontal4(orb.desc, theme)
			}
			else if (char == '-')
			{
				var orb = swappable as? Orb
				if (orb == null) orb = Orb(Orb.getRandomOrb(this), theme)
				tile.special = Vertical4(orb.desc, theme)
			}
			else if (char == '@')
			{
				swappable!!.sealCount = sealStrength
			}
			else if (symbolMap.containsKey(char.toInt()))
			{
				val symbol = symbolMap[char.toInt()]
				if (symbol.sinkableDesc != null)
				{
					val sinkable = Sinkable(symbol.sinkableDesc.sprite.copy(), theme)
					tile.sinkable = sinkable
				}

				if (symbol.extends != ' ')
				{
					modifyOrbs(tile, symbol.extends)
				}

				if (symbol.seal > 0)
				{
					swappable!!.sealCount = symbol.seal
				}

				if (symbol.attack > 0)
				{
					var orb = swappable as? Orb
					if (orb == null) orb = Orb(Orb.getRandomOrb(this), theme)
					orb.attackTimer = symbol.attack
					orb.hasAttack = true
					tile.orb = orb
				}

				var orb = swappable as? Orb
				if (orb == null) orb = Orb(Orb.getRandomOrb(this), theme)

				val special = when (symbol.special)
				{
					"3x3" -> DualMatch(orb.desc, theme)
					"4x4" -> DoubleDualMatch(orb.desc, theme)
					"V4" -> Vertical4(orb.desc, theme)
					"3V4" -> DualVert(orb.desc, theme)
					"H4" -> Horizontal4(orb.desc, theme)
					"3H4" -> DualHori(orb.desc, theme)
					"5" -> Match5(orb.desc, theme)
					"All" -> Match5Dual(orb.desc, theme)
					else -> null
				}

				if (special != null)
				{
					tile.special = special
				}

				if (symbol.container != null && tile.contents != null)
				{
					tile.container = Container(symbol.container.sprite.copy(), symbol.container.hp, tile.contents!!)
					tile.container!!.alwaysShowHP = symbol.container.alwaysShowHP
				}
			}
		}

		for (x in 0 until charGrid.xSize)
		{
			for (y in 0 until charGrid.ySize)
			{
				val tile = grid.grid[x, y]
				val char = charGrid[x, y]

				modifyOrbs(tile, char)
			}
		}

		for (victory in victoryConditions)
		{
			if (victory is CompletionConditionSink)
			{
				val numToSink = (victory as? CompletionConditionSink)!!.count

				val existingCount = grid.grid.filter { it.sinkable != null || it.container?.contents is Sinkable }.count()

				val chests = grid.grid.filter { it.chest != null || it.container?.contents is Chest }.map { it.chest ?: it.container!!.contents as Chest }.toList().toGdxArray()

				val totalToSpawn = numToSink - existingCount
				if (totalToSpawn > 0)
				{
					for (i in 0 until totalToSpawn)
					{
						chests.random()!!.numToSpawn++
					}
				}
			}
		}

		defeatConditions.forEach{ it.attachHandlers(grid) }
		victoryConditions.forEach{ it.attachHandlers(grid) }
	}

	fun update(delta: Float)
	{
		val done = grid.update(delta)

		if (!completed && done)
		{
			if (victoryConditions.all { it.isCompleted() } || defeatConditions.any { it.isCompleted() })
			{
				completeFun = {complete()}
				completed = true
				onComplete()
			}

			if (completed && completeFun != null)
			{
				Future.call(completeFun!!, 0.5f, this)
			}
		}

		if (completed && completeFun != null && (!done || Mote.motes.size > 0))
		{
			Future.call(completeFun!!, 0.5f, this)
		}
	}

	fun complete()
	{
		completeFun = null
		if (victoryConditions.all { it.isCompleted() })
		{
			victoryAction.invoke()
		}
		else if (defeatConditions.any { it.isCompleted() })
		{
			defeatAction.invoke()
		}
	}

	fun copy(): Level
	{
		val base = load(loadPath)[0]
		base.charGrid = charGrid
		return base
	}

	companion object
	{
		fun load(path: String): Array<Level>
		{
			val xml = getXml(path)

			val levels = Array<Level>()

			val symbolsMap = IntMap<Symbol>()
			val symbolsEl = xml.getChildByName("Symbols")
			if (symbolsEl != null)
			{
				for (symbolEl in symbolsEl.children)
				{
					val character = symbolEl.get("Character")[0]
					val extends = symbolEl.get("Extends", " ")!!.firstOrNull() ?: ' '
					var sprite: SpriteWrapper? = null

					val nameKey = symbolEl.get("NameKey", null)

					val symbolSpriteEl = symbolEl.getChildByName("Sprite")
					if (symbolSpriteEl != null)
					{
						sprite = SpriteWrapper.load(symbolSpriteEl)
					}

					val blockEl = symbolEl.getChildByName("Block")
					var blockDesc: BlockDesc? = null
					if (blockEl != null && blockEl.childCount > 0)
					{
						val blockSpriteEl = blockEl.getChildByName("Sprite")
						val blockSprite = if(blockSpriteEl != null) AssetManager.loadSprite(blockSpriteEl) else null
						val blockHP = blockEl.getInt("Health", 1)
						val showHP = blockEl.getBoolean("AlwaysShowHP", false)

						blockDesc = BlockDesc(blockSprite, blockHP, showHP)
					}

					val plate = symbolEl.getInt("Plate", 0)
					val seal = symbolEl.getInt("Seal", 0)
					val attack = symbolEl.getInt("Attack", 0)

					val isMonster = symbolEl.getBoolean("IsMonster", false)
					val monsterDescEl = symbolEl.getChildByName("MonsterDesc")
					var monsterDesc: MonsterDesc? = null
					if (monsterDescEl != null)
					{
						monsterDesc = MonsterDesc.load(monsterDescEl)
					}

					val special = symbolEl.get("SpecialOrb")

					var sinkableDesc: SinkableDesc? = null
					val sinkableDescEl = symbolEl.getChildByName("Sinkable")
					if (sinkableDescEl != null)
					{
						val sinkSprite = AssetManager.loadSprite(sinkableDescEl.getChildByName("Sprite")!!)
						sinkableDesc = SinkableDesc(sinkSprite)
					}

					val isChest = symbolEl.getBoolean("IsChest", false)

					var containerDesc: ContainerDesc? = null
					val containerDescEl = symbolEl.getChildByName("Container")
					if (containerDescEl != null)
					{
						val containerSprite = AssetManager.loadSprite(containerDescEl.getChildByName("Sprite")!!)
						containerDesc = ContainerDesc(containerSprite, containerDescEl.getInt("Health", 1), containerDescEl.getBoolean("AlwaysShowHP", false))
					}

					var spreader: Spreader? = null
					val spreaderEl = symbolEl.getChildByName("Spreader")
					if (spreaderEl != null)
					{
						spreader = Spreader.load(spreaderEl)
					}

					val type = SymbolType.valueOf(symbolEl.get("Type", "Floor")!!.toUpperCase())

					symbolsMap[character.toInt()] = Symbol(character, extends, nameKey, sprite, blockDesc, plate, seal, attack, isMonster, monsterDesc, special, sinkableDesc, isChest, containerDesc, spreader, type)
				}
			}

			val grid = xml.getChildByName("Grid")!!
			for (ci in 0 until grid.childCount * 2)
			{
				val cel = grid.getChild(ci / 2)

				val level = Level(path)
				level.symbolMap.putAll(symbolsMap)

				var rows = cel

				if (rows.name == "Path" || !rows.text.isBlank())
				{
					// We are importing from another file
					val gridPath = rows.text
					val gridxml = getXml(gridPath)
					rows = gridxml.getChildByName("Rows")!!
				}

				val flip = (ci % 2) == 0

				val width = rows.getChild(0).text.length
				val height = rows.childCount
				level.charGrid = Array2D<Char>(width, height) { x, y -> rows.getChild(y).text[if (flip) width - x - 1 else x] }

				val defeatsEl = xml.getChildByName("AllowedDefeats")!!
				for (defeatEl in defeatsEl.children)
				{
					level.defeatConditions.add(AbstractCompletionCondition.load(defeatEl))
				}

				val victoriesEl = xml.getChildByName("AllowedVictories")!!
				for (victoryEl in victoriesEl.children)
				{
					level.victoryConditions.add(AbstractCompletionCondition.load(victoryEl))
				}

				level.orbs = xml.getInt("OrbCount", 6)

				level.sealStrength = xml.getInt("SealStrength", 1)
				level.blockStrength = xml.getInt("BlockStrength", 1)

				level.factions.addAll(xml.get("Faction", "")!!.split(",").asSequence())

				val customMonsterDesc = xml.getChildByName("CustomMonster")
				if (customMonsterDesc != null)
				{
					level.customMonster = MonsterDesc.load(customMonsterDesc)
				}

				levels.add(level)
			}

			return levels
		}
	}
}

data class SinkableDesc(val sprite: Sprite)

data class ContainerDesc(val sprite: Sprite, val hp: Int, val alwaysShowHP: Boolean)

data class BlockDesc(val sprite: Sprite?, val hp: Int, val alwaysShowHP: Boolean)

enum class SymbolType
{
	FLOOR,
	WALL,
	PIT
}
data class Symbol(
		val char: Char, val extends: Char,
		val nameKey: String?,
		val sprite: SpriteWrapper?,
		val block: BlockDesc?, val plate: Int, val seal: Int, val attack: Int,
		val isMonster: Boolean, val monsterDesc: MonsterDesc?,
		val special: String,
		val sinkableDesc: SinkableDesc?,
		val isChest: Boolean,
		val container: ContainerDesc?,
		val spreader: Spreader?,
		val type: SymbolType)