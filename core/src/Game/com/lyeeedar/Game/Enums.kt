package com.lyeeedar

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.vectorToAngle

// ----------------------------------------------------------------------
enum class Rarity
{
	BRONZE,
	SILVER,
	GOLD;


	companion object
	{
		val Values = Rarity.values()
	}
}

// ----------------------------------------------------------------------
enum class SpawnWeight
{
	ANY,
	START,
	STARTMIDDLE,
	MIDDLE,
	MIDDLEEND,
	END;

	val subWeights = Array<SpawnWeight>()

	companion object
	{
		val Values = SpawnWeight.values()

		init
		{
			ANY.subWeights.add(START, MIDDLE, END)

			START.subWeights.add(START)

			STARTMIDDLE.subWeights.add(START, MIDDLE)

			MIDDLE.subWeights.add(MIDDLE)

			MIDDLEEND.subWeights.add(MIDDLE, END)

			END.subWeights.add(END)
		}
	}
}

// ----------------------------------------------------------------------
enum class Statistic private constructor(val min: Float, val max: Float, val tooltip: String)
{
	HEALTH(1f, Float.MAX_VALUE, "The number of attacks you can take before dieing"),
	MATCHDAMAGE(0f, Float.MAX_VALUE, "The bonus damage you do the first time you match next to a damageable object each turn"),
	ABILITYDAMAGE(0f, Float.MAX_VALUE, "The bonus damage you do with specials and abilities"),
	PIERCE(0f, Float.MAX_VALUE, "The amount of damage resistance you remove each time you hit something with damage resistance"),
	POWERGAIN(0f, Float.MAX_VALUE, "The bonus power you gain the first time you gain power each turn"),
	BONUSGOLD(-Float.MAX_VALUE, Float.MAX_VALUE, "The bonus multiplier you gain each time you gain gold"),
	HASTE(-Float.MAX_VALUE, Float.MAX_VALUE, "How much faster you move. This increases turn count, delay between monster attacks and abilities, and buff durations"),
	REGENERATION(0f, Float.MAX_VALUE, "The amount of health you regenerate each turn"),
	BERSERK(0f, Float.MAX_VALUE, "When below half health this is the amount MatchDamage, AbilityDamage and PowerGain is increased"),
	PRICEREDUCTION(0f, Float.MAX_VALUE, "The percentage reduction in price of items from a shop"),
	AEGIS(0f, 1f, "The chance to block the damage from an attack as it hits"),
	COUNTER(0f, 1f, "When taking damage from an attack, the chance to hit back at a random enemy"),
	REFLECT(0f, 1f, "The chance to block the damage from an attack, then hit back at a random enemy"),
	CHAOTICNATURE(0f, 1f, "The percentage increase or decrease to randomly apply to each statistic. Changes whenever you complete an encounter"),
	WEAKNESSAURA(0f, Float.MAX_VALUE, "The value to subtract from enemy life as they spawn"),
	NECROTICAURA(0f, Float.MAX_VALUE, "The amount of health you gain each time you kill an enemy"),
	BUFFDURATION(0f, Float.MAX_VALUE, "The multiplier to increase buff duration by"),
	LUCK(-1f, 1f, "Affects your chance of gaining rewards");

	companion object
	{
		val Values = Statistic.values()

		fun parse(xmlData: XmlData, statistics: FastEnumMap<Statistic, Float>)
		{
			for (stat in Values)
			{
				var value = statistics[stat] ?: 0f
				value = xmlData.getFloat(stat.toString(), value)
				statistics[stat] = value
			}
		}
	}
}

// ----------------------------------------------------------------------
enum class EquipmentSlot
{
	HEAD,
	MAINHAND,
	OFFHAND,
	BODY;

	companion object
	{
		val Values = EquipmentSlot.values()
	}
}