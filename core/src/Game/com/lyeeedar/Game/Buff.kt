package com.lyeeedar.Game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.CardWidget
import com.lyeeedar.UI.Seperator
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.addTapToolTip
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.XmlData

class Buff(val xml: XmlData)
{
	var name: String = xml.get("Name")
	var icon: Sprite = AssetManager.loadSprite(xml.getChildByName("Icon")!!)
	val statistics = FastEnumMap<Statistic, Float>(Statistic::class.java)

	var remainingDuration: Int = xml.getInt("Duration")

	init
	{
		Statistic.parse(xml.getChildByName("Statistics")!!, statistics)
	}

	fun getCard(): CardWidget
	{
		val card = CardWidget(CardWidget.createFrontTable(name, icon.copy()), createTable(), AssetManager.loadTextureRegion("GUI/BuffCardback")!!, this)
		return card
	}

	fun getCardSmall(isBuff: Boolean): CardWidget
	{
		val basicTable = Table()

		val icon = if (isBuff)
			AssetManager.loadSprite("GUI/Buff")
		else
			AssetManager.loadSprite("GUI/Debuff")

		basicTable.add(SpriteWidget(icon, 64f, 64f)).grow()
		basicTable.row()

		val card = CardWidget(basicTable, createTable(), icon.currentTexture, this)
		return card
	}

	fun createTable(showTurns: Boolean = true): Table
	{
		val table = Table()
		table.defaults().growX()

		val titleStack = Stack()
		val iconTable = Table()
		iconTable.add(SpriteWidget(icon, 64f, 64f)).expandX().right().pad(5f)
		titleStack.add(iconTable)
		titleStack.add(Label(name, Statics.skin, "cardtitle"))

		table.add(titleStack).growX()
		table.row()

		table.add(Seperator(Statics.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f)
		table.row()

		if (showTurns)
		{
			table.add(Label("Remaining Duration: $remainingDuration", Statics.skin, "card")).pad(5f)
			table.row()
		}

		if (statistics.any { it != 0f })
		{
			table.add(Seperator(Statics.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f)
			table.row()

			table.add(Label("Statistics", Statics.skin, "cardtitle"))
			table.row()

			for (stat in Statistic.Values)
			{
				val statVal = statistics[stat] ?: 0f

				val statTable = Table()
				statTable.add(Label(stat.toString().toLowerCase().capitalize() + ": ", Statics.skin, "card")).expandX().left()
				statTable.add(Label(statVal.toString(), Statics.skin, "card"))
				statTable.addTapToolTip(stat.tooltip)

				var add = false

				if (statVal != 0f)
				{
					add = true
				}

				if (statVal > 0)
				{
					val diff = statVal
					val diffLabel = Label("+" + diff.toString(), Statics.skin, "cardwhite")
					diffLabel.color = Color.GREEN
					statTable.add(diffLabel)
				}
				else if (statVal < 0)
				{
					val diff = statVal
					val diffLabel = Label(diff.toString(), Statics.skin, "cardwhite")
					diffLabel.color = Color.RED
					statTable.add(diffLabel)
				}

				if (add)
				{
					table.add(statTable)
					table.row()
				}
			}
		}

		return table
	}

	fun copy(): Buff
	{
		return Buff(xml)
	}

	fun save(kryo: Kryo, output: Output)
	{
		kryo.writeObject(output, xml)
		output.writeInt(remainingDuration, true)
	}

	companion object
	{
		fun load(xml: XmlData): Buff
		{
			val buff = Buff(xml)
			return buff
		}

		fun load(kryo: Kryo, input: Input): Buff
		{
			val xml = kryo.readObject(input, XmlData::class.java)
			val buff = load(xml)
			buff.remainingDuration = input.readInt(true)
			return buff
		}
	}
}