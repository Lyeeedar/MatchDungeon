package com.lyeeedar.Util

import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.Serializer
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Game.*

fun Kryo.registerGameSerializers()
{
	val kryo = this

	kryo.register(Player::class.java, object : Serializer<Player>()
	{
		override fun read(kryo: Kryo, input: Input, type: Class<Player>): Player
		{
			return Player.load(kryo, input, Global.deck)
		}

		override fun write(kryo: Kryo, output: Output, player: Player)
		{
			player.save(kryo, output)
		}
	})

	kryo.register(GlobalDeck::class.java, object : Serializer<GlobalDeck>()
	{
		override fun read(kryo: Kryo, input: Input, type: Class<GlobalDeck>): GlobalDeck
		{
			return GlobalDeck.load(input)
		}

		override fun write(kryo: Kryo, output: Output, deck: GlobalDeck)
		{
			deck.save(output)
		}
	})

	kryo.register(GameStateFlags::class.java, object : Serializer<GameStateFlags>()
	{
		override fun read(kryo: Kryo, input: Input, type: Class<GameStateFlags>): GameStateFlags
		{
			return GameStateFlags.load(kryo, input)
		}

		override fun write(kryo: Kryo, output: Output, flags: GameStateFlags)
		{
			flags.save(kryo, output)
		}
	})

	kryo.register(Quest::class.java, object : Serializer<Quest>()
	{
		override fun read(kryo: Kryo, input: Input, type: Class<Quest>): Quest
		{
			return Quest.load(input)
		}

		override fun write(kryo: Kryo, output: Output, quest: Quest)
		{
			quest.save(output)
		}
	})

	kryo.register(CardContent::class.java, object : Serializer<CardContent>()
	{
		override fun read(kryo: Kryo, input: Input, type: Class<CardContent>): CardContent
		{
			return CardContent.load(kryo, input)
		}

		override fun write(kryo: Kryo, output: Output, content: CardContent)
		{
			content.save(kryo, output)
		}
	})
}