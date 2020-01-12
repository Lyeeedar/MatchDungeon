package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.exp4j.Helpers.evaluate
import com.lyeeedar.Board.CompletionCondition.AbstractCompletionCondition
import com.lyeeedar.Board.CompletionCondition.CompletionConditionCustomOrb
import com.lyeeedar.Board.CompletionCondition.CompletionConditionSink
import com.lyeeedar.Components.MonsterEffectComponent
import com.lyeeedar.Components.matchable
import com.lyeeedar.Components.monsterEffect
import com.lyeeedar.Components.renderable
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Global
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
	var chosenFaction: Faction? = null

	var customMonster: MonsterDesc? = null

	val spawnList = Array<String>()

	// Active state data
	var levelTheme: Theme? = null
	var questTheme: Theme? = null
	val theme: Theme
		get() = levelTheme ?: questTheme!!

	lateinit var grid: Grid
	lateinit var player: Player
	var completed = false
	var completeFun: (() -> Unit)? = null
	val onComplete = Event0Arg()

	lateinit var victoryAction: () -> Unit
	lateinit var defeatAction: () -> Unit

	fun spawnOrb(): Entity
	{
		val toSpawn = spawnList.random()

		if (toSpawn == "Changer")
		{
			val orb = createOrb(OrbDesc.getRandomOrb(this), theme)
			orb.matchable()!!.isChanger = true
			return orb
		}
		else if (toSpawn == "Attack")
		{
			val orb = createOrb(OrbDesc.getRandomOrb(this), theme)
			val monsterEffect = MonsterEffect(MonsterEffectType.ATTACK, ObjectMap())
			monsterEffect.timer = 7
			addMonsterEffect(orb, monsterEffect)

			return orb
		}
		else if (toSpawn == "Summon")
		{
			val data = ObjectMap<String, Any>()
			data["FACTION"] = factions.random()

			val orb = createOrb(OrbDesc.getRandomOrb(this), theme)
			val monsterEffect = MonsterEffect(MonsterEffectType.SUMMON, data)
			monsterEffect.timer = 7
			addMonsterEffect(orb, monsterEffect)

			return orb
		}
		else if (toSpawn == "Orb")
		{
			return createOrb(OrbDesc.getRandomOrb(this), theme)
		}
		else
		{
			return createOrb(OrbDesc.getNamedOrb(toSpawn), theme)
		}
	}

	fun create(questTheme: Theme, player: Player, victoryAction: () -> Unit, defeatAction: () -> Unit)
	{
		this.questTheme = questTheme

		if (spawnList.size == 0)
		{
			spawnList.addAll(questTheme.spawnList)
		}

		if (spawnList.size == 1)
		{
			for (i in 0 until 9)
			{
				spawnList.add("Orb")
			}
		}

		val spawnWeightTotal = spawnList.size
		for (victory in victoryConditions)
		{
			if (victory is CompletionConditionCustomOrb)
			{
				val chance = victory.orbChance
				val weight = (spawnWeightTotal.toFloat() * chance).ciel()

				for (i in 0 until weight)
				{
					spawnList.add(victory.targetOrbName)
				}
			}
		}

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
				tile.groundSprite = theme.floor.copy()
				tile.wallSprite = theme.wall.copy()
			}
			else if (char == '~')
			{
				tile.canHaveOrb = false
				tile.isPit = true
				tile.wallSprite = theme.pit.copy()
			}
			else if (char == 'p')
			{
				tile.canHaveOrb = true
				tile.groundSprite = theme.floor.copy()
				tile.plateStrength = plateStrength
			}
			else if (char == '=')
			{
				tile.canHaveOrb = true
				tile.groundSprite = theme.floor.copy()
				tile.block = Block(theme)
				tile.block!!.maxhp = blockStrength
			}
			else if (char == '$')
			{
				tile.chest = Chest(true, theme)
				tile.canHaveOrb = false
				tile.groundSprite = theme.floor.copy()
				tile.chest!!.attachHandlers(grid)
			}
			else if (char == '£')
			{
				tile.chest = Chest(false, theme)
				tile.canHaveOrb = false
				tile.groundSprite = theme.floor.copy()
				tile.chest!!.attachHandlers(grid)
			}
			else if (char == '!')
			{
				tile.canHaveOrb = true
				tile.groundSprite = theme.floor.copy()

				hasMonster = true
			}
			else if (char == '?')
			{
				tile.canHaveOrb = true
				tile.groundSprite = theme.floor.copy()

				hasMonster = true
			}
			else if (symbolMap.containsKey(char.toInt()))
			{
				val symbol = symbolMap[char.toInt()]
				if (symbol.extends != ' ')
				{
					loadTile(tile, symbol.extends)
				}
				else if (char != '.')
				{
					loadTile(tile, '.')
				}

				if (symbol.usageCondition.evaluate(Global.getVariableMap()) == 0f)
				{
					loadTile(tile, symbol.fallbackChar)
					return
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
					for (effect in symbol.block.onTurnEffects)
					{
						tile.block!!.onTurnEffects.add(effect.copy())
					}
				}

				tile.plateStrength = symbol.plate

				for (effect in symbol.onTurnEffects)
				{
					tile.onTurnEffects.add(effect.copy())
				}

				if (symbol.isMonster)
				{
					hasMonster = true
				}
				else if (symbol.monsterDesc != null)
				{
					hasMonster = true
				}

				if (symbol.type == SymbolType.PIT)
				{
					tile.canHaveOrb = false
					tile.isPit = true

					if (symbol.sprite != null)
					{
						tile.wallSprite = symbol.sprite.copy()
					}
				}
				else if (symbol.type == SymbolType.WALL)
				{
					tile.canHaveOrb = false
					tile.isPit = false

					if (symbol.sprite != null)
					{
						tile.wallSprite = symbol.sprite.copy()
					}
				}
				else
				{
					tile.canHaveOrb = true
					tile.isPit = false

					if (symbol.sprite != null)
					{
						tile.groundSprite = symbol.sprite.copy()
					}
				}

				if (symbol.friendlyDesc != null)
				{
					val friendly = Friendly(symbol.friendlyDesc)
					friendly.setTile(tile, grid)
				}

				if (symbol.isChest)
				{
					tile.chest = Chest(true, theme)
					tile.groundSprite = theme.floor.copy()
					tile.chest!!.attachHandlers(grid)

					tile.canHaveOrb = false
					tile.isPit = false
				}

				if (symbol.spreader != null)
				{
					tile.spreader = symbol.spreader.copy()
				}

				tile.nameKey = symbol.nameKey?.toUpperCase()
			}
			else
			{
				tile.groundSprite = theme.floor.copy()
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

		val chosenFactionName = factions.random()
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

		if (hasMonster)
		{
			// iterate through and find groups
			data class MonsterMarker(val monsterDesc: MonsterDesc?, val isBoss: Boolean, val difficulty: Int = 0, var used: Boolean = false)
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

					if (symbol.factionMonster != null)
					{
						monster = MonsterMarker(null, symbol.factionMonster.isBoss, symbol.factionMonster.difficultyModifier)
					}
					else if (symbol.isMonster || symbol.monsterDesc != null)
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
						val monsterDesc = monsterMarker.monsterDesc ?: if (monsterMarker.isBoss) chosenFaction!!.getBoss(size) else chosenFaction!!.get(size)

						var difficulty = monsterMarker.difficulty
						if (monsterDesc.size < size)
						{
							difficulty += 3
						}

						val monster = Monster(monsterDesc, difficulty)
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
					val sprite = if (symbol.sinkableDesc.usePlayer) Global.player.baseCharacter.sprite.copy() else symbol.sinkableDesc.sprite!!.copy()

					val sinkable = Sinkable(sprite, theme)
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
					val orb = swappable as? Matchable
					val desc = orb?.desc ?: Orb.getRandomOrb(this)

					val monsterEffect = MonsterEffect(MonsterEffectType.ATTACK, ObjectMap(), desc, theme)
					monsterEffect.timer = symbol.attack

					tile.monsterEffect = monsterEffect
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

					for (effect in symbol.container.onTurnEffects)
					{
						tile.container!!.onTurnEffects.add(effect.copy())
					}
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
		Global.player.levelbuffs.clear()
		Global.player.leveldebuffs.clear()
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

					val usageCondition = symbolEl.get("UsageCondition", "1")!!.toLowerCase()
					val fallbackChar = symbolEl.get("FallbackCharacter", ".")!!.firstOrNull() ?: '.'

					val nameKey = symbolEl.get("NameKey", null)

					var sprite: SpriteWrapper? = null
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
						val turnEffects = TurnEffect.loadFromElement(blockEl.getChildByName("TurnEffects"))

						blockDesc = BlockDesc(blockSprite, blockHP, showHP, turnEffects)
					}

					val plate = symbolEl.getInt("Plate", 0)
					val seal = symbolEl.getInt("Seal", 0)
					val attack = symbolEl.getInt("Attack", 0)

					var friendlyDesc: FriendlyDesc? = null
					val friendlyDescEl = symbolEl.getChildByName("Friendly")
					if (friendlyDescEl != null)
					{
						friendlyDesc = FriendlyDesc.load(friendlyDescEl)
					}

					val isMonster = symbolEl.getBoolean("IsMonster", false)

					val factionMonsterEl = symbolEl.getChildByName("FactionMonster")
					var factionMonster: FactionMonster? = null
					if (factionMonsterEl != null)
					{
						val isBoss = factionMonsterEl.getBoolean("IsBoss", false)
						val difficulty = factionMonsterEl.getInt("DifficultyModifier", 0)
						factionMonster = FactionMonster(isBoss, difficulty)
					}

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
						val spriteDesc = sinkableDescEl.getChildByName("Sprite")
						val sprite = if (spriteDesc != null) AssetManager.loadSprite(spriteDesc) else null

						val usePlayerSprite = sinkableDescEl.getBoolean("UsePlayerSprite", false)

						sinkableDesc = SinkableDesc(sprite, usePlayerSprite)
					}

					val isChest = symbolEl.getBoolean("IsChest", false)

					var containerDesc: ContainerDesc? = null
					val containerDescEl = symbolEl.getChildByName("Container")
					if (containerDescEl != null)
					{
						val containerSprite = AssetManager.loadSprite(containerDescEl.getChildByName("Sprite")!!)
						val hp = containerDescEl.getInt("Health", 1)
						val alwaysShowHP = containerDescEl.getBoolean("AlwaysShowHP", false)
						val turnEffects = TurnEffect.loadFromElement(containerDescEl.getChildByName("TurnEffects"))

						containerDesc = ContainerDesc(containerSprite, hp, alwaysShowHP, turnEffects)
					}

					var spreader: Spreader? = null
					val spreaderEl = symbolEl.getChildByName("Spreader")
					if (spreaderEl != null)
					{
						spreader = Spreader.load(spreaderEl)
					}

					val turnEffects = TurnEffect.loadFromElement(symbolEl.getChildByName("TurnEffects"))

					val type = SymbolType.valueOf(symbolEl.get("Type", "Floor")!!.toUpperCase())

					symbolsMap[character.toInt()] = Symbol(
							character, extends,
							usageCondition, fallbackChar,
							nameKey,
							sprite,
							turnEffects,
							blockDesc, plate, seal, attack,
							friendlyDesc,
							isMonster, factionMonster, monsterDesc,
							special, sinkableDesc,
							isChest, containerDesc,
							spreader,
							type)
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

				val spawnWeightsOverrideEl = xml.getChildByName("SpawnWeightsOverride")
				if (spawnWeightsOverrideEl != null)
				{
					val spawnWeightsEl = spawnWeightsOverrideEl.getChildByName("SpawnWeights")!!
					for (el in spawnWeightsEl.children)
					{
						val split = el.text.split(",")

						for (i in 0 until split[1].toInt())
						{
							level.spawnList.add(split[0])
						}
					}
				}

				level.sealStrength = xml.getInt("SealStrength", 1)
				level.blockStrength = xml.getInt("BlockStrength", 1)

				level.factions.addAll(xml.get("Faction", "")!!.split(",").asSequence())

				if (xml.get("Theme", null) != null)
				{
					level.levelTheme = Theme.Companion.load("Themes/" + xml.get("Theme"))
				}

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

data class SinkableDesc(val sprite: Sprite?, val usePlayer: Boolean)

data class ContainerDesc(val sprite: Sprite, val hp: Int, val alwaysShowHP: Boolean, val onTurnEffects: Array<TurnEffect>)

data class BlockDesc(val sprite: Sprite?, val hp: Int, val alwaysShowHP: Boolean, val onTurnEffects: Array<TurnEffect>)

data class FactionMonster(val isBoss: Boolean, val difficultyModifier: Int)

enum class SymbolType
{
	FLOOR,
	WALL,
	PIT
}
data class Symbol(
		val char: Char, val extends: Char,
		val usageCondition: String, val fallbackChar: Char,
		val nameKey: String?,
		val sprite: SpriteWrapper?,
		val onTurnEffects: Array<TurnEffect>,
		val block: BlockDesc?, val plate: Int, val seal: Int, val attack: Int,
		val friendlyDesc: FriendlyDesc?,
		val isMonster: Boolean, val factionMonster: FactionMonster?, val monsterDesc: MonsterDesc?,
		val special: String,
		val sinkableDesc: SinkableDesc?,
		val isChest: Boolean,
		val container: ContainerDesc?,
		val spreader: Spreader?,
		val type: SymbolType)

fun loadDataBlock(xmlData: XmlData): ObjectMap<String, Any>
{
	val data = ObjectMap<String, Any>()

	for (el in xmlData.children)
	{
		if (el.name == "Spreader")
		{
			val spreader = Spreader.load(el)
			data[el.name.toUpperCase()] = spreader
		}
		else if (el.name == "Debuff")
		{
			val buff = Buff.load(el)
			data[el.name.toUpperCase()] = buff
		}
		else if (el.name == "MonsterDesc")
		{
			data[el.name.toUpperCase()] = MonsterDesc.load(el)
		}
		else if (el.name.contains("Effect"))
		{
			data[el.name.toUpperCase()] = AssetManager.loadParticleEffect(el)
		}
		else if (el.name == "Sprite")
		{
			data[el.name.toUpperCase()] = AssetManager.loadSprite(el)
		}
		else
		{
			data[el.name.toUpperCase()] = el.text.toUpperCase()
		}
	}

	return data
}