package com.lyeeedar.Game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.IntSet
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.GameStateFlags
import com.lyeeedar.Global
import com.lyeeedar.Util.registerGdxSerialisers
import com.lyeeedar.Util.registerLyeeedarSerialisers
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class Save
{
	var maxhpStat: Int = 0
	var powerGainStat: Int = 0
	var attackDamStat: Int = 0
	var abilityDamStat: Int = 0
	var gold: Int = 0

	var equippedAbilities = Array(4) { e -> "" }
	var unlockedAbilities = IntSet()

	var flags: GameStateFlags = GameStateFlags()

	fun saveData()
	{
		maxhpStat = Global.player.maxhpStat
		powerGainStat = Global.player.powerGainStat
		attackDamStat = Global.player.attackDamStat
		abilityDamStat = Global.player.abilityDamStat
		gold = Global.player.gold

		for (i in 0 until 4)
		{
			equippedAbilities[i] = Global.player.abilities[i]?.key ?: ""
		}

		for (ability in Global.player.abilityTree.descendants())
		{
			if (ability.value.bought)
			{
				unlockedAbilities.add(ability.key)
			}
		}

		flags = Global.flags
	}

	fun loadData()
	{
		Global.player.maxhpStat = maxhpStat
		Global.player.powerGainStat = powerGainStat
		Global.player.attackDamStat = attackDamStat
		Global.player.abilityDamStat = abilityDamStat
		Global.player.gold = gold

		for (ability in Global.player.abilityTree.descendants())
		{
			ability.value.bought = unlockedAbilities.contains(ability.key)
		}

		for (i in 0 until 4)
		{
			if (!equippedAbilities[i].isBlank())
			{
				Global.player.abilities[i] = Global.player.getAbility(equippedAbilities[i])
			}
		}

		Global.flags = flags
	}

	companion object
	{
		val kryo: Kryo by lazy { initKryo() }
		fun initKryo(): Kryo
		{
			val kryo = Kryo()
			kryo.isRegistrationRequired = false

			kryo.registerGdxSerialisers()
			kryo.registerLyeeedarSerialisers()

			return kryo
		}

		fun save()
		{
			val attemptFile = Gdx.files.local("save.dat")

			var output: Output? = null
			try
			{
				output = Output(GZIPOutputStream(attemptFile.write(false)))
			}
			catch (e: Exception)
			{
				e.printStackTrace()
				return
			}

			val save = Save()
			save.saveData()

			kryo.writeObject(output, save)

			output.close()
		}

		fun load(): Boolean
		{
			var input: com.esotericsoftware.kryo.io.Input? = null
			var save: Save? = null

			try
			{
				input = com.esotericsoftware.kryo.io.Input(GZIPInputStream(Gdx.files.local("save.dat").read()))
				save = kryo.readObject(input, Save::class.java)
			}
			catch (e: Exception)
			{
				e.printStackTrace()
				return false
			}
			finally
			{
				input?.close()
			}

			save?.loadData()

			return true
		}
	}
}