package com.lyeeedar

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.*

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
enum class Statistic private constructor(val min: Float, val max: Float, val icon: Sprite)
{
	HEALTH(1f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/heart_red_full")),
	MATCHDAMAGE(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/weapon_broadsword")),
	ABILITYDAMAGE(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/weapon_magic_staff_chaos")),
	PIERCE(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/weapon_spear")),
	POWERGAIN(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/potion_blue")),
	BONUSGOLD(-Float.MAX_VALUE, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/coin_gold")),
	HASTE(-Float.MAX_VALUE, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/armor_cloth_boot")),
	REGENERATION(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/heart_green_full")),
	BERSERK(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/skull_small_blood")),
	PRICEREDUCTION(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/coin_copper")),
	AEGIS(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/shield_iron_buckler")),
	COUNTER(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/armor_wild_chest")),
	REFLECT(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/armor_mystic_chest")),
	CHAOTICNATURE(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/necklace_dark")),
	WEAKNESSAURA(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/crystal_dragon")),
	NECROTICAURA(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/skull_large_closed")),
	BUFFDURATION(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/necklace_mystic")),
	LUCK(-1f, 1f, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/gem_tourmaline")),
	VAMPIRICSTRIKES(0f, Float.MAX_VALUE, AssetManager.loadGrayscaleSprite("Oryx/uf_split/uf_items/weapon_magic_sword_hellfire"));

	val niceName: String
		get()
		{
			return when(this)
			{
				HEALTH -> Localisation.getText("statistic.health", "UI")
				MATCHDAMAGE -> Localisation.getText("statistic.matchdamage", "UI")
				ABILITYDAMAGE -> Localisation.getText("statistic.abilitydamage", "UI")
				PIERCE -> Localisation.getText("statistic.pierce", "UI")
				POWERGAIN -> Localisation.getText("statistic.powergain", "UI")
				BONUSGOLD -> Localisation.getText("statistic.bonusgold", "UI")
				HASTE -> Localisation.getText("statistic.haste", "UI")
				REGENERATION -> Localisation.getText("statistic.regeneration", "UI")
				BERSERK -> Localisation.getText("statistic.berserk", "UI")
				PRICEREDUCTION -> Localisation.getText("statistic.pricereduction", "UI")
				AEGIS -> Localisation.getText("statistic.aegis", "UI")
				COUNTER -> Localisation.getText("statistic.counter", "UI")
				REFLECT -> Localisation.getText("statistic.reflect", "UI")
				CHAOTICNATURE -> Localisation.getText("statistic.chaoticnature", "UI")
				WEAKNESSAURA -> Localisation.getText("statistic.weaknessaura", "UI")
				NECROTICAURA -> Localisation.getText("statistic.necroticaura", "UI")
				BUFFDURATION -> Localisation.getText("statistic.buffduration", "UI")
				LUCK -> Localisation.getText("statistic.luck", "UI")
				VAMPIRICSTRIKES -> Localisation.getText("statistic.vampiricstrikes", "UI")
			}
		}

	val tooltip: String
		get()
		{
			return when(this)
			{
				HEALTH -> Localisation.getText("statistic.health.description", "UI")
				MATCHDAMAGE -> Localisation.getText("statistic.matchdamage.description", "UI")
				ABILITYDAMAGE -> Localisation.getText("statistic.abilitydamage.description", "UI")
				PIERCE -> Localisation.getText("statistic.pierce.description", "UI")
				POWERGAIN -> Localisation.getText("statistic.powergain.description", "UI")
				BONUSGOLD -> Localisation.getText("statistic.bonusgold.description", "UI")
				HASTE -> Localisation.getText("statistic.haste.description", "UI")
				REGENERATION -> Localisation.getText("statistic.regeneration.description", "UI")
				BERSERK -> Localisation.getText("statistic.berserk.description", "UI")
				PRICEREDUCTION -> Localisation.getText("statistic.pricereduction.description", "UI")
				AEGIS -> Localisation.getText("statistic.aegis.description", "UI")
				COUNTER -> Localisation.getText("statistic.counter.description", "UI")
				REFLECT -> Localisation.getText("statistic.reflect.description", "UI")
				CHAOTICNATURE -> Localisation.getText("statistic.chaoticnature.description", "UI")
				WEAKNESSAURA -> Localisation.getText("statistic.weaknessaura.description", "UI")
				NECROTICAURA -> Localisation.getText("statistic.necroticaura.description", "UI")
				BUFFDURATION -> Localisation.getText("statistic.buffduration.description", "UI")
				LUCK -> Localisation.getText("statistic.luck.description", "UI")
				VAMPIRICSTRIKES -> Localisation.getText("statistic.vampiricstrikes.description", "UI")
			}
		}

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