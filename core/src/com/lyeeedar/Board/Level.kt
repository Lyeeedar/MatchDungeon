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

		if (toSpawn == "Shield")
		{
			return Shield(theme)
		}
		else if (toSpawn == "Changer")
		{
			val orb = Orb(Orb.getRandomOrb(this), theme)
			orb.isChanger = true
			orb.nextDesc = Orb.getRandomOrb(this, orb.desc)
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
				tile.block!!.count = blockStrength
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
					tile.spriteSetter = symbol.sprite
				}

				if (symbol.block > 0)
				{
					tile.block = Block(theme)
					tile.block!!.count = symbol.block
				}

				tile.plateStrength = symbol.plate

				if (symbol.shield > 0)
				{
					tile.shield = Shield(theme)
					tile.shield!!.count = symbol.shield
				}

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
			data class Block(val monsterDesc: MonsterDesc?, val tiles: Array<Tile>)
			val blocks = Array<Block>()

			fun loadMonster(tile: Tile, char: Char)
			{
				var isMonster = false
				var monsterDesc: MonsterDesc? = null

				if (char == '!')
				{
					isMonster = true
				}
				else if (symbolMap.containsKey(char.toInt()))
				{
					val symbol = symbolMap[char.toInt()]
					if (symbol.extends != ' ')
					{
						loadMonster(tile, symbol.extends)
					}

					if (symbol.isMonster)
					{
						isMonster = true
						monsterDesc = symbol.monsterDesc
					}
				}

				if (isMonster)
				{
					var found = false
					for (block in blocks)
					{
						if (block.monsterDesc == monsterDesc)
						{
							for (testtile in block.tiles)
							{
								if (testtile.dist(tile) == 1)
								{
									block.tiles.add(tile)
									found = true
									break
								}
							}
						}
					}

					if (!found)
					{
						val newBlock = Block(monsterDesc, Array())

						newBlock.tiles.add(tile)
						blocks.add(newBlock)
					}
				}
			}

			for (x in 0 until charGrid.xSize)
			{
				for (y in 0 until charGrid.ySize)
				{
					val tile = grid.grid[x, y]
					loadMonster(tile, charGrid[x, y])
				}
			}

			// convert groups into x by x arrays
			for (block in blocks)
			{
				var minx = block.tiles[0].x
				var miny = block.tiles[0].y
				var maxx = block.tiles[0].x
				var maxy = block.tiles[0].y

				for (tile in block.tiles)
				{
					if (tile.x < minx) minx = tile.x
					if (tile.y < miny) miny = tile.y
					if (tile.x > maxx) maxx = tile.x
					if (tile.y > maxy) maxy = tile.y
				}

				val w = (maxx - minx) + 1
				val h = (maxy - miny) + 1

				if (w != h) throw Exception("Non-square monster!")

				val size = w
				val monsterDesc = block.monsterDesc ?: chosenFaction!!.get(size)
				val monster = Monster(monsterDesc)
				monster.size = size

				for (x in 0 until size)
				{
					for (y in 0 until size)
					{
						val gx = minx + x
						val gy = miny + y

						val tile = grid.grid[gx, gy]

						tile.monster = monster
						monster.tiles[x, y] = tile
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
				orb.special = Horizontal4(orb)
				tile.orb = orb
			}
			else if (char == '-')
			{
				var orb = swappable as? Orb
				if (orb == null) orb = Orb(Orb.getRandomOrb(this), theme)
				orb.special = Vertical4(orb)
				tile.orb = orb
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
					val sinkable = Sinkable(symbol.sinkableDesc.sprite, theme)
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
					"3x3" -> DualMatch(orb)
					"V4" -> Vertical4(orb)
					"H4" -> Horizontal4(orb)
					"5" -> Match5(orb)
					else -> null
				}

				if (special != null)
				{
					orb.special = special
					tile.orb = orb
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

				val existingCount = grid.grid.filter { it.sinkable != null }.count()

				val chests = grid.grid.filter { it.chest != null }.map { it.chest!! }

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
			if (victoryConditions.all { it.isCompleted() } || defeatConditions.all { it.isCompleted() })
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

		if (completed && completeFun != null && (!done || Mote.moteCount > 0))
		{
			Future.call(completeFun!!, 0.5f, this)
		}
	}

	private fun complete()
	{
		completeFun = null
		if (victoryConditions.all { it.isCompleted() })
		{
			victoryAction.invoke()
		}
		else if (defeatConditions.all { it.isCompleted() })
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

					val symbolSpriteEl = symbolEl.getChildByName("Sprite")
					if (symbolSpriteEl != null)
					{
						sprite = SpriteWrapper.load(symbolSpriteEl)
					}

					val block = symbolEl.getInt("Block", 0)
					val plate = symbolEl.getInt("Plate", 0)
					val seal = symbolEl.getInt("Seal", 0)
					val shield = symbolEl.getInt("Shield", 0)
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

					val type = SymbolType.valueOf(symbolEl.get("Type", "Floor")!!.toUpperCase())

					symbolsMap[character.toInt()] = Symbol(character, extends, sprite, block, plate, seal, shield, attack, isMonster, monsterDesc, special, sinkableDesc, type)
				}
			}

			val grid = xml.getChildByName("Grid")!!
			for (ci in 0 until grid.childCount)
			{
				val cel = grid.getChild(ci)

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

				val width = rows.getChild(0).text.length
				val height = rows.childCount
				level.charGrid = Array2D<Char>(width, height) { x, y -> rows.getChild(y).text[x] }

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

enum class SymbolType
{
	FLOOR,
	WALL,
	PIT
}
data class Symbol(val char: Char, val extends: Char, val sprite: SpriteWrapper?, val block: Int, val plate: Int, val seal: Int, val shield: Int, val attack: Int, val isMonster: Boolean, val monsterDesc: MonsterDesc?, val special: String, val sinkableDesc: SinkableDesc?, val type: SymbolType)