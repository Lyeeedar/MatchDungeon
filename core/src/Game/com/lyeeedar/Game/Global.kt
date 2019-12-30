package com.lyeeedar.Game

import com.badlogic.gdx.utils.ObjectFloatMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Card.Card
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Screens.DeckScreen
import com.lyeeedar.Screens.QuestScreen
import com.lyeeedar.Screens.QuestSelectionScreen
import com.lyeeedar.Statistic
import com.lyeeedar.Util.Settings
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.filename
import ktx.collections.addAll

/**
 * Created by Philip on 04-Jul-16.
 */

class Global
{
	companion object
	{
		lateinit var player: Player
		var globalflags = GameStateFlags()
		var questflags = GameStateFlags()
		var cardflags = GameStateFlags()

		lateinit var deck: GlobalDeck

		fun setup()
		{
			deck = GlobalDeck()
			Statics.setup()
		}

		fun newGame()
		{
			deck = GlobalDeck()
			deck.newGame()

			player = deck.getPlayer()
			globalflags = GameStateFlags()
			questflags = GameStateFlags()
			cardflags = GameStateFlags()

			Statics.settings = Settings()

			val quest = Quest.load("Intro/Intro")

			Statics.game.getTypedScreen<QuestScreen>()?.setup(quest)
			Statics.game.getTypedScreen<DeckScreen>()?.setup()
			Statics.game.getTypedScreen<QuestSelectionScreen>()?.setup()

			QuestScreen.instance.swapTo()
		}

		fun getVariableMap(): ObjectFloatMap<String>
		{
			val output = ObjectFloatMap<String>()
			output.putAll(globalflags.flags)
			output.putAll(questflags.flags)
			output.putAll(cardflags.flags)

			output.put("money", player.gold.toFloat())

			for (stat in Statistic.Values)
			{
				output.put(stat.toString().toLowerCase(), player.getStat(stat))
			}

			for (slot in EquipmentSlot.Values)
			{
				output.put(slot.toString().toLowerCase(), if(player.equipment[slot] != null) 1f else 0f)
			}

			for (quest in deck.quests)
			{
				val theme = quest.theme.path.filename(false)
				output.put("Unlocked$theme", 1f)
			}

			output.put(player.baseCharacter.name.toLowerCase(), 1f)

			return output
		}
	}
}

class GlobalDeck
{
	val encounters = com.lyeeedar.Util.UniqueArray<Card>({ it.path.hashCode() })
	val equipment = com.lyeeedar.Util.UniqueArray<Equipment>({ it.path.hashCode() })
	val characters = com.lyeeedar.Util.UniqueArray<Character>({ it.path.hashCode() })
	val quests = com.lyeeedar.Util.UniqueArray<Quest>({ it.path.hashCode() })

	val newencounters = com.lyeeedar.Util.UniqueArray<Card>({ it.path.hashCode() })
	val newequipment = com.lyeeedar.Util.UniqueArray<Equipment>({ it.path.hashCode() })
	val newcharacters = com.lyeeedar.Util.UniqueArray<Character>({ it.path.hashCode() })
	val newquests = com.lyeeedar.Util.UniqueArray<Quest>({ it.path.hashCode() })

	var hasNewEncounters = false
	var hasNewEquipment = false
	var hasNewCharacters = false

	val playerDeck = PlayerDeck()
	lateinit var chosenCharacter: Character

	fun newGame()
	{
		for (cardPath in com.lyeeedar.Util.XmlData.enumeratePaths("Cards/Default", "Card"))
		{
			val card = Card.load(cardPath)
			encounters.add(card)
		}

		for (equipPath in com.lyeeedar.Util.XmlData.enumeratePaths("Equipment/Default", "MainHand"))
		{
			val equip = Equipment.load(equipPath)
			equipment.add(equip)
		}

		for (equipPath in com.lyeeedar.Util.XmlData.enumeratePaths("Equipment/Default", "OffHand"))
		{
			val equip = Equipment.load(equipPath)
			equipment.add(equip)
		}

		for (equipPath in com.lyeeedar.Util.XmlData.enumeratePaths("Equipment/Default", "Body"))
		{
			val equip = Equipment.load(equipPath)
			equipment.add(equip)
		}

		for (equipPath in com.lyeeedar.Util.XmlData.enumeratePaths("Equipment/Default", "Head"))
		{
			val equip = Equipment.load(equipPath)
			equipment.add(equip)
		}

		characters.add(Character.load("Peasant"))
		chosenCharacter = characters.first()

		playerDeck.equipment.addAll(equipment)
		playerDeck.encounters.addAll(encounters)
	}

	fun getPlayer(): Player
	{
		return Player(chosenCharacter, playerDeck.copy())
	}

	fun clear()
	{
		encounters.clear()
		equipment.clear()
		characters.clear()
		quests.clear()

		playerDeck.encounters.clear()
		playerDeck.equipment.clear()

		characters.add(Character.load("Peasant"))
		chosenCharacter = characters.first()
	}

	fun save(output: Output)
	{
		output.writeInt(encounters.size)
		for (encounter in encounters)
		{
			encounter.save(output)
		}

		output.writeInt(equipment.size)
		for (equip in equipment)
		{
			equip.save(output)
		}

		output.writeInt(characters.size)
		for (character in characters)
		{
			character.save(output)
		}

		output.writeInt(quests.size)
		for (quest in quests)
		{
			quest.save(output)
		}

		output.writeInt(newencounters.size)
		for (encounter in newencounters)
		{
			output.writeInt(encounter.path.hashCode())
		}

		output.writeInt(newequipment.size)
		for (equip in newequipment)
		{
			output.writeInt(equip.path.hashCode())
		}

		output.writeInt(newcharacters.size)
		for (character in newcharacters)
		{
			output.writeInt(character.path.hashCode())
		}

		output.writeInt(newquests.size)
		for (quest in newquests)
		{
			output.writeInt(quest.path.hashCode())
		}

		output.writeInt(playerDeck.encounters.size)
		for (encounter in playerDeck.encounters)
		{
			output.writeInt(encounter.path.hashCode())
		}

		output.writeInt(playerDeck.equipment.size)
		for (equip in playerDeck.equipment)
		{
			output.writeInt(equip.path.hashCode())
		}

		output.writeInt(chosenCharacter.path.hashCode())

		output.writeBoolean(hasNewCharacters)
		output.writeBoolean(hasNewEncounters)
		output.writeBoolean(hasNewEquipment)
	}

	companion object
	{
		fun load(input: Input): GlobalDeck
		{
			val deck = GlobalDeck()

			val numEncounters = input.readInt()
			for (i in 0 until numEncounters)
			{
				val card = Card.load(input)
				deck.encounters.add(card)
			}

			val numEquipment = input.readInt()
			for (i in 0 until numEquipment)
			{
				val equip = Equipment.load(input)
				deck.equipment.add(equip)
			}

			val numCharacters = input.readInt()
			for (i in 0 until numCharacters)
			{
				val character = Character.load(input)
				deck.characters.add(character)
			}

			val numQuests = input.readInt()
			for (i in 0 until numQuests)
			{
				val quest = Quest.load(input)
				deck.quests.add(quest)
			}

			val numNewEncounters = input.readInt()
			for (i in 0 until numNewEncounters)
			{
				val hash = input.readInt()
				deck.newencounters.add(deck.encounters.uniqueMap[hash])
			}

			val numNewEquipment = input.readInt()
			for (i in 0 until numNewEquipment)
			{
				val hash = input.readInt()
				deck.newequipment.add(deck.equipment.uniqueMap[hash])
			}

			val numNewCharacters = input.readInt()
			for (i in 0 until numNewCharacters)
			{
				val hash = input.readInt()
				deck.newcharacters.add(deck.characters.uniqueMap[hash])
			}

			val numNewQuests = input.readInt()
			for (i in 0 until numNewQuests)
			{
				val hash = input.readInt()
				deck.newquests.add(deck.quests.uniqueMap[hash])
			}

			val numPlayerEncounters = input.readInt()
			for (i in 0 until numPlayerEncounters)
			{
				val hash = input.readInt()
				deck.playerDeck.encounters.add(deck.encounters.uniqueMap[hash])
			}

			val numPlayerEquipment = input.readInt()
			for (i in 0 until numPlayerEquipment)
			{
				val hash = input.readInt()
				deck.playerDeck.equipment.add(deck.equipment.uniqueMap[hash])
			}

			val chosenCharacterHash = input.readInt()
			deck.chosenCharacter = deck.characters.uniqueMap[chosenCharacterHash]

			deck.hasNewCharacters = input.readBoolean()
			deck.hasNewEncounters = input.readBoolean()
			deck.hasNewEquipment = input.readBoolean()

			return deck
		}
	}
}

class GameStateFlags
{
	val flags = ObjectFloatMap<String>()

	fun save(kryo: Kryo, output: Output)
	{
		kryo.writeObject(output, flags)
	}

	companion object
	{
		fun load(kryo: Kryo, input: Input): GameStateFlags
		{
			val gsf = GameStateFlags()

			val newflags = kryo.readObject(input, ObjectFloatMap::class.java)

			for (pair in newflags)
			{
				gsf.flags.put(pair.key as String, pair.value)
			}

			return gsf
		}
	}
}

