package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Localisation
import com.lyeeedar.Util.Statics

class Player(val baseCharacter: Character, val deck: PlayerDeck)
{
	var gold: Int = 0
	var isInBerserkRange = false

	var statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	var equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	var buffs = Array<Buff>()

	var choaticNature = FastEnumMap<Statistic, Float>(Statistic::class.java)

	var levelbuffs = Array<Buff>()
	var leveldebuffs = Array<Buff>()

	init
	{
		for (slot in EquipmentSlot.Values)
		{
			equipment[slot] = baseCharacter.equipment[slot]?.copy()
		}
	}

	fun getStat(statistic: Statistic, withChoaticNature: Boolean = true): Float
	{
		var stat = baseCharacter.baseStatistics[statistic] ?: 0f
		stat += statistics[statistic] ?: 0f

		if (withChoaticNature && statistic != Statistic.CHAOTICNATURE)
		{
			stat += choaticNature[statistic] ?: 0f
		}

		for (slot in EquipmentSlot.Values)
		{
			val equip = equipment[slot]
			if (equip != null)
			{
				stat += equip.statistics[statistic] ?: 0f
			}
		}

		for (buff in buffs)
		{
			stat += buff.statistics[statistic] ?: 0f
		}

		for (buff in levelbuffs)
		{
			stat += buff.statistics[statistic] ?: 0f
		}

		for (buff in leveldebuffs)
		{
			stat += buff.statistics[statistic] ?: 0f
		}

		if (statistic == Statistic.MATCHDAMAGE || statistic == Statistic.ABILITYDAMAGE || statistic == Statistic.POWERGAIN || statistic == Statistic.PIERCE)
		{
			if (isInBerserkRange)
			{
				if (statistic == Statistic.PIERCE)
				{
					stat += getStat(Statistic.BERSERK) * 0.5f
				}
				else
				{
					stat += getStat(Statistic.BERSERK)
				}
			}
		}

		return clamp(stat, statistic.min, statistic.max)
	}

	fun getEquippedSet(): ObjectSet<String>
	{
		val output = ObjectSet<String>()

		for (slot in EquipmentSlot.Values)
		{
			val equip = equipment[slot] ?: continue
			output.add(equip.path)
		}

		return output
	}

	fun getCard(): CardWidget
	{
		val table = Table()
		table.defaults().growX()

		val descLabel = Label(baseCharacter.description, Statics.skin, "card")
		descLabel.setWrap(true)

		table.add(descLabel).growX().pad(10f)
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard"))
		table.row()

		val statisticsButton = TextButton(Localisation.getText("statistics", "UI"), Statics.skin)
		statisticsButton.addClickListener {
			val statisticsTable = Table()

			var bright = true
			for (stat in Statistic.Values)
			{
				val statVal = getStat(stat)
				if (statVal != 0f)
				{
					val statTable = Table()
					if (bright)
					{
						statTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
					}
					bright = !bright

					statTable.add(SpriteWidget(stat.icon.copy(), 16f, 16f)).pad(5f)
					statTable.add(Label("${stat.niceName}:", Statics.skin, "card")).expandX().left().pad(5f)
					statTable.add(Label("%.1f".format(statVal), Statics.skin, "card")).pad(5f)

					val statSources = Array<Pair<String, Float>>()
					statSources.add(Pair(Localisation.getText("rewards", "UI"), statistics[stat] ?: 0f))
					statSources.add(Pair(Localisation.getText("equipment", "UI"), EquipmentSlot.Values.map { equipment[it]?.statistics?.get(stat) ?: 0f }.sum()))
					statSources.add(Pair(Localisation.getText("buffs", "UI"), buffs.map { it.statistics[stat] ?: 0f }.sum()))
					statSources.add(Pair(Localisation.getText("statistic.chaoticnature", "UI"), choaticNature[stat] ?: 0f))
					statSources.add(Pair(Localisation.getText("levelbuffs", "UI"), levelbuffs.map { it.statistics[stat] ?: 0f }.sum()))
					statSources.add(Pair(Localisation.getText("leveldebuffs", "UI"), leveldebuffs.map { it.statistics[stat] ?: 0f }.sum()))

					val base = baseCharacter.baseStatistics[stat] ?: 0f
					var eqn = Localisation.getText("base", "UI") + "(${base})"
					for (source in statSources)
					{
						if (source.second != 0f)
						{
							eqn += " + ${source.first}(${source.second})"
						}
					}

					statTable.addTapToolTip("Total($statVal) = \n${eqn}\n\n${stat.tooltip}")

					statisticsTable.add(statTable).growX()
					statisticsTable.row()
				}
			}

			FullscreenTable.createCard(
					Localisation.getText("statistics", "UI"),
					Localisation.getText("player", "UI"),
					statisticsTable,
					statisticsButton.localToStageCoordinates(Vector2()))
		}

		table.add(statisticsButton).growX().pad(10f)
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard"))
		table.row()

		if (buffs.size > 0)
		{
			val buffsButton = TextButton(Localisation.getText("buffs", "UI"), Statics.skin)
			buffsButton.addClickListener {
				val buffsTable = Table()

				val bufftable = Table()
				buffsTable.add(bufftable).grow()
				buffsTable.row()

				var i = 0
				for (buff in buffs)
				{
					val card = buff.getCard()
					card.setSize(25f, 45f)
					card.setFacing(true, false)

					bufftable.add(card).pad(0f, 5f, 0f, 5f).size(25f, 45f)

					i++
					if (i == 6)
					{
						i = 0
						bufftable.row()
					}
				}

				FullscreenTable.createCard(
					Localisation.getText("buffs", "UI"),
						Localisation.getText("player", "UI"),
						buffsTable,
						buffsButton.localToStageCoordinates(Vector2()))
			}

			table.add(buffsButton).growX().pad(10f)
			table.row()

			table.add(Seperator(Statics.skin, "horizontalcard"))
			table.row()
		}

		val equipmentButton = TextButton(Localisation.getText("equipment", "UI"), Statics.skin)
		equipmentButton.addClickListener {
			val equipmentTable = Table()

			val emptySlot = AssetManager.loadSprite("Icons/Empty")

			for (slot in EquipmentSlot.Values)
			{
				val equipment = equipment[slot]

				if (equipment == null)
				{
					val equipTable = Table()
					equipmentTable.add(equipTable).growX().padBottom(2f)
					equipmentTable.row()

					equipTable.add(Label(Localisation.getText("none", "UI"), Statics.skin,"card"))
					equipTable.add(SpriteWidget(emptySlot, 32f, 32f)).size(32f).expandX().right()
				}
				else
				{
					val equipTable = Table()
					equipmentTable.add(equipTable).growX().padBottom(2f)
					equipmentTable.row()

					equipTable.add(Label(equipment.name, Statics.skin, "card"))

					val infoButton = Button(Statics.skin, "infocard")
					infoButton.setSize(24f, 24f)
					infoButton.addClickListener {
						FullscreenTable.createCard(equipment.getCard(null, false), infoButton.localToStageCoordinates(Vector2(12f, 12f)))
					}
					equipTable.add(infoButton).size(24f).pad(0f, 12f, 0f, 12f).expandX().right()

					val iconStack = Stack()
					iconStack.add(SpriteWidget(emptySlot, 32f, 32f))
					iconStack.add(SpriteWidget(equipment.icon, 32f, 32f))
					equipTable.add(iconStack).size(32f)
				}
			}

			FullscreenTable.createCard(
				Localisation.getText("equipment", "UI"),
					Localisation.getText("player", "UI"),
					equipmentTable,
					equipmentButton.localToStageCoordinates(Vector2()))
		}

		table.add(equipmentButton).growX().pad(10f)
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard"))
		table.row()

		return CardWidget.createCard(baseCharacter.name, Localisation.getText("player", "UI"), Sprite(baseCharacter.sprite.textures[0]), table, AssetManager.loadTextureRegion("GUI/CharacterCardback")!!)
	}

	fun save(kryo: Kryo, output: Output)
	{
		output.writeInt(gold)
		kryo.writeObject(output, statistics)

		for (slot in EquipmentSlot.Values)
		{
			val equip = equipment[slot]
			if (equip == null)
			{
				output.writeBoolean(false)
			}
			else
			{
				output.writeBoolean(true)
				equip.save(output)
			}
		}

		output.writeInt(buffs.size)
		for (buff in buffs)
		{
			buff.save(kryo, output)
		}

		output.writeInt(deck.encounters.size)
		for (encounter in deck.encounters)
		{
			output.writeInt(encounter.path.hashCode())
		}

		output.writeInt(deck.equipment.size)
		for (equip in deck.equipment)
		{
			output.writeInt(equip.path.hashCode())
		}
	}

	fun getAbilityIndex(ability: Ability): Int
	{
		var i = 0
		for (slot in EquipmentSlot.Values)
		{
			val equip = equipment[slot] ?: continue
			if (equip.ability == ability)
			{
				return i
			}

			i++
		}

		return -1
	}

	companion object
	{
		fun load(kryo: Kryo, input: Input, deck: GlobalDeck): Player
		{
			val gold = input.readInt()
			val stats = kryo.readObject(input, FastEnumMap::class.java) as FastEnumMap<Statistic, Float>

			val equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)
			for (slot in EquipmentSlot.Values)
			{
				val exists = input.readBoolean()
				if (exists)
				{
					val equip = Equipment.load(input)
					equipment[slot] = equip
				}
			}

			val buffs = Array<Buff>()
			val numBuffs = input.readInt()
			for (i in 0 until numBuffs)
			{
				val buff = Buff.load(kryo, input)
				buffs.add(buff)
			}

			val playerDeck = PlayerDeck()
			val numPlayerEncounters = input.readInt()
			for (i in 0 until numPlayerEncounters)
			{
				val hash = input.readInt()
				playerDeck.encounters.add(deck.encounters.uniqueMap[hash])
			}

			val numPlayerEquipment = input.readInt()
			for (i in 0 until numPlayerEquipment)
			{
				val hash = input.readInt()
				playerDeck.equipment.add(deck.equipment.uniqueMap[hash])
			}

			val player = Player(deck.chosenCharacter, playerDeck)
			player.gold = gold
			player.statistics = stats
			player.equipment = equipment
			player.buffs.addAll(buffs)

			return player
		}
	}
}

class PlayerDeck
{
	val encounters = Array<Card>()
	val equipment = Array<Equipment>()

	fun copy(): PlayerDeck
	{
		val deck = PlayerDeck()
		deck.encounters.addAll(encounters)
		deck.equipment.addAll(equipment)

		return deck
	}
}