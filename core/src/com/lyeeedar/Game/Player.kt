package com.lyeeedar.Game

import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Global
import com.lyeeedar.GlobalDeck
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap

class Player(val baseCharacter: Character, val deck: PlayerDeck)
{
	var gold: Int = 0
	var isInBerserkRange = false

	var statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)
	var equipment = FastEnumMap<EquipmentSlot, Equipment>(EquipmentSlot::class.java)

	var buffs = Array<Buff>()

	var choaticNature = FastEnumMap<Statistic, Float>(Statistic::class.java)

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
			val equip = getEquipment(slot)
			if (equip != null)
			{
				stat += equip.statistics[statistic] ?: 0f
			}
		}

		for (buff in buffs)
		{
			stat += buff.statistics[statistic] ?: 0f
		}

		if (statistic == Statistic.MATCHDAMAGE || statistic == Statistic.ABILITYDAMAGE || statistic == Statistic.POWERGAIN)
		{
			if (isInBerserkRange)
			{
				stat += getStat(Statistic.BERSERK)
			}
		}

		return clamp(stat, statistic.min, statistic.max)
	}

	fun getEquipment(equipmentSlot: EquipmentSlot): Equipment?
	{
		return equipment[equipmentSlot] ?: baseCharacter.equipment[equipmentSlot]
	}

	fun getEquippedSet(): ObjectSet<String>
	{
		val output = ObjectSet<String>()

		for (slot in EquipmentSlot.Values)
		{
			val equip = getEquipment(slot) ?: continue
			output.add(equip.path)
		}

		return output
	}

	fun createTable(): Table
	{
		val table = Table()
		table.defaults().growX()

		val titleStack = Stack()
		val iconTable = Table()
		iconTable.add(SpriteWidget(baseCharacter.sprite.copy(), 64f, 64f)).left().pad(5f)
		titleStack.add(iconTable)
		titleStack.add(Label(baseCharacter.name, Global.skin, "cardtitle"))

		table.add(titleStack).growX()
		table.row()

		val descLabel = Label(baseCharacter.description, Global.skin, "card")
		descLabel.setWrap(true)

		table.add(descLabel).growX()
		table.row()

		table.add(Seperator(Global.skin, "horizontalcard"))
		table.row()

		table.add(Label("Statistics", Global.skin, "cardtitle"))
		table.row()

		for (stat in Statistic.Values)
		{
			val basestat = (baseCharacter.baseStatistics[stat] ?: 0f) + (statistics[stat] ?: 0f)
			val truestat = getStat(stat)

			if (truestat != 0f || basestat != 0f)
			{
				val diff = truestat - basestat
				val diffStr: String
				if (diff > 0)
				{
					diffStr = "[GREEN]+$diff[]"
				}
				else if (diff < 0)
				{
					diffStr = "[RED]$diff[]"
				}
				else
				{
					diffStr = ""
				}

				val statTable = Table()
				statTable.add(Label(stat.toString().toLowerCase().capitalize() + ":", Global.skin, "card")).expandX().left()
				statTable.add(Label(truestat.toString() + " (" + basestat + diffStr + ")", Global.skin, "card"))
				statTable.addTapToolTip(stat.tooltip)

				table.add(statTable).growX()
				table.row()
			}
		}

		table.add(Seperator(Global.skin, "horizontalcard"))
		table.row()

		if (buffs.size > 0)
		{
			table.add(Label("Buffs", Global.skin, "cardtitle"))
			table.row()

			val bufftable = Table()
			table.add(bufftable).growX().height(50f)
			table.row()

			for (buff in buffs)
			{
				val card = buff.getCard()
				card.setSize(25f, 45f)
				card.setFacing(true, false)

				bufftable.add(card).pad(0f, 5f, 0f, 5f).size(25f, 45f)
			}

			table.add(Seperator(Global.skin, "horizontalcard"))
			table.row()
		}

		table.add(Label("Equipment", Global.skin, "cardtitle"))
		table.row()

		val emptySlot = AssetManager.loadSprite("Icons/Empty")

		for (slot in EquipmentSlot.Values)
		{
			val equipment = getEquipment(slot)

			if (equipment == null)
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label("None", Global.skin,"card"))
				equipTable.add(SpriteWidget(emptySlot, 32f, 32f)).size(32f).expandX().right()
			}
			else
			{
				val equipTable = Table()
				table.add(equipTable).growX().padBottom(2f)
				table.row()

				equipTable.add(Label(equipment.name, Global.skin, "card"))

				val infoButton = Button(Global.skin, "infocard")
				infoButton.setSize(24f, 24f)
				infoButton.addClickListener {
					val t = equipment.createTable(null, false)

					FullscreenTable.createCard(t, infoButton.localToStageCoordinates(Vector2(12f, 12f)))
				}
				equipTable.add(infoButton).size(24f).pad(0f, 12f, 0f, 12f).expandX().right()

				val iconStack = Stack()
				iconStack.add(SpriteWidget(emptySlot, 32f, 32f))
				iconStack.add(SpriteWidget(equipment.icon, 32f, 32f))
				equipTable.add(iconStack).size(32f)
			}
		}

		return table
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