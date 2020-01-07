package com.lyeeedar

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.XmlData

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
enum class Statistic private constructor(val niceName: String, val min: Float, val max: Float, val tooltip: String, val icon: Sprite)
{
	HEALTH("Health", 1f, Float.MAX_VALUE, "The number of attacks you can take before dieing", AssetManager.loadSprite("Oryx/uf_split/uf_items/heart_red_full")),
	MATCHDAMAGE("Match Damage", 0f, Float.MAX_VALUE, "The bonus damage you do the first time you match next to a damageable object each turn", AssetManager.loadSprite("Oryx/uf_split/uf_items/weapon_broadsword")),
	ABILITYDAMAGE("Ability Damage", 0f, Float.MAX_VALUE, "The bonus damage you do with specials and abilities", AssetManager.loadSprite("Oryx/uf_split/uf_items/weapon_magic_staff_chaos")),
	PIERCE("Pierce", 0f, Float.MAX_VALUE, "The amount of damage resistance you remove each time you hit something with damage resistance", AssetManager.loadSprite("Oryx/uf_split/uf_items/weapon_spear")),
	POWERGAIN("Power Gain", 0f, Float.MAX_VALUE, "The bonus power you gain the first time you gain power each turn", AssetManager.loadSprite("Oryx/uf_split/uf_items/potion_blue")),
	BONUSGOLD("Bonus Gold", -Float.MAX_VALUE, Float.MAX_VALUE, "The bonus multiplier you gain each time you gain gold", AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_gold")),
	HASTE("Haste", -Float.MAX_VALUE, Float.MAX_VALUE, "How much faster you move. This increases turn count, delay between monster attacks and abilities, and buff durations", AssetManager.loadSprite("Oryx/uf_split/uf_items/armor_cloth_boot")),
	REGENERATION("Regeneration", 0f, Float.MAX_VALUE, "The amount of health you regenerate each turn", AssetManager.loadSprite("Oryx/uf_split/uf_items/heart_green_full")),
	BERSERK("Berserk", 0f, Float.MAX_VALUE, "When below half health this is the amount MatchDamage, AbilityDamage and PowerGain is increased", AssetManager.loadSprite("Oryx/uf_split/uf_items/skull_small_blood")),
	PRICEREDUCTION("Price Reduction", 0f, Float.MAX_VALUE, "The percentage reduction in price of items from a shop", AssetManager.loadSprite("Oryx/uf_split/uf_items/coin_copper")),
	AEGIS("Aegis", 0f, 1f, "The chance to block the damage from an attack as it hits", AssetManager.loadSprite("Oryx/uf_split/uf_items/shield_iron_buckler")),
	COUNTER("Counter", 0f, 1f, "When taking damage from an attack, the chance to hit back at a random enemy", AssetManager.loadSprite("Oryx/uf_split/uf_items/armor_wild_chest")),
	REFLECT("Reflect", 0f, 1f, "The chance to block the damage from an attack, then hit back at a random enemy", AssetManager.loadSprite("Oryx/uf_split/uf_items/armor_mystic_chest")),
	CHAOTICNATURE("Chaotic Nature", 0f, 1f, "The percentage increase or decrease to randomly apply to each statistic. Changes whenever you complete an encounter", AssetManager.loadSprite("Oryx/uf_split/uf_items/necklace_dark")),
	WEAKNESSAURA("Weakness Aura", 0f, Float.MAX_VALUE, "The value to subtract from enemy life as they spawn", AssetManager.loadSprite("Oryx/uf_split/uf_items/crystal_dragon")),
	NECROTICAURA("Necrotic Aura", 0f, Float.MAX_VALUE, "The amount of health you gain each time you kill an enemy", AssetManager.loadSprite("Oryx/uf_split/uf_items/skull_large_closed")),
	BUFFDURATION("Buff Duration", 0f, Float.MAX_VALUE, "The multiplier to increase buff duration by", AssetManager.loadSprite("Oryx/uf_split/uf_items/necklace_mystic")),
	LUCK("Luck", -1f, 1f, "Affects your chance of gaining rewards", AssetManager.loadSprite("Oryx/uf_split/uf_items/gem_tourmaline"));

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

		enum class DisplayType
		{
			FLAT,
			COMPARISON,
			MODIFIER
		}
		fun createTable(stats: FastEnumMap<Statistic, Float>, type: DisplayType, other: FastEnumMap<Statistic, Float>? = null): Table
		{
			val table = Table()

			var bright = true
			for (stat in Values)
			{
				val statVal = stats[stat] ?: 0f
				val otherStatVal = other?.get(stat) ?: 0f

				if (statVal != 0f || otherStatVal != 0f)
				{
					val statTable = Table()

					if (bright)
					{
						statTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(1f, 1f, 1f, 0.1f))
					}
					bright = !bright

					statTable.add(SpriteWidget(stat.icon.copy(), 16f, 16f)).pad(5f)
					statTable.add(Label("${stat.niceName}: ", Statics.skin, "card")).expandX().left().pad(5f)
					statTable.addTapToolTip(stat.tooltip)

					when (type)
					{
						DisplayType.FLAT -> {
							statTable.add(Label(statVal.toString(), Statics.skin, "card")).pad(5f)
						}
						DisplayType.MODIFIER -> {
							if (statVal > 0)
							{
								val diff = statVal
								val diffLabel = Label("+$diff", Statics.skin, "cardwhite")
								diffLabel.color = Color(0f, 0.5f, 0f, 1f)
								statTable.add(diffLabel).pad(5f)
							}
							else if (statVal < 0)
							{
								val diff = statVal
								val diffLabel = Label(diff.toString(), Statics.skin, "cardwhite")
								diffLabel.color = Color(0.5f, 0f, 0f, 1f)
								statTable.add(diffLabel).pad(5f)
							}
						}
						DisplayType.COMPARISON -> {
							statTable.add(Label(statVal.toString(), Statics.skin, "card")).pad(5f)
							if (otherStatVal < statVal)
							{
								val diff = statVal - otherStatVal
								val diffLabel = Label("+$diff", Statics.skin, "cardwhite")
								diffLabel.color = Color(0f, 0.5f, 0f, 1f)
								statTable.add(diffLabel).pad(5f)
							}
							else if (statVal < otherStatVal)
							{
								val diff = otherStatVal - statVal
								val diffLabel = Label("-$diff", Statics.skin, "cardwhite")
								diffLabel.color = Color(0.5f, 0f, 0f, 1f)
								statTable.add(diffLabel).pad(5f)
							}
						}
					}

					table.add(statTable).growX()
					table.row()
				}
			}

			return table
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