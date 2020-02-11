package com.lyeeedar.Screens

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Board.*
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTurns
import com.lyeeedar.Components.isBasicOrb
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Global
import com.lyeeedar.Game.Player
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Localisation
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.random

/**
 * Created by Philip on 20-Mar-16.
 */

class GridScreen(): AbstractScreen()
{
	val hp_full: Sprite = AssetManager.loadSprite("GUI/health_full")
	val hp_empty: Sprite = AssetManager.loadSprite("GUI/health_empty")
	val emptySlot = AssetManager.loadSprite("Icons/Empty")
	var hpBar = Table()
	var launchButton: TextButton? = null
	var refreshButton: TextButton? = null
	var ultimateButton: TextButton? = null
	var completeButton: TextButton? = null
	var grid: GridWidget? = null
	lateinit var level: Level
	val defeatTable = Table()
	val victoryTable = Table()

	val buffTable = Table()
	val debuffTable = Table()

	var timeMultiplier = 1f

	// ----------------------------------------------------------------------
	init
	{
		instance = this
	}

	// ----------------------------------------------------------------------
	override fun create()
	{
		if (Statics.debug)
		{
			debugConsole.register("complete", "") { args, console ->
				level.victoryAction.invoke()

				true
			}

			debugConsole.register("fail", "") { args, console ->
				level.defeatAction.invoke()

				true
			}

			debugConsole.register("setturns", "", fun(args, console): Boolean
			{
				val condition = level.defeatConditions.filterIsInstance<CompletionConditionTurns>().firstOrNull()

				if (condition == null)
				{
					console.error("No turns defeat condition!")
					return false
				}

				val count = args[0].toInt()

				condition.turnCount = count

				return true
			})

			debugConsole.register("suicide", "", fun(args, console): Boolean
			{
				val die = level.defeatConditions.filterIsInstance<CompletionConditionDie>().firstOrNull()

				if (die == null)
				{
					console.error("No die defeat condition!")
					return false
				}

				die.hp = 0

				return true
			})

			debugConsole.register("maxpower", "") { args, console ->
				PowerBar.instance.power = PowerBar.instance.maxPower

				true
			}

			debugConsole.register("spawn", "", fun(args, console): Boolean {

				val special: Special
				if (args[0] == "match5" || args[0] == "gem")
				{
					special = Match5()
				}
				else if (args[0] == "hori")
				{
					special = Horizontal4()
				}
				else if (args[0] == "vert")
				{
					special = Vertical4()
				}
				else if (args[0] == "cross")
				{
					special = Cross()
				}
				else if (args[0] == "dualmatch")
				{
					special = DualMatch()
				}
				else
				{
					return false
				}

				val tile = level.grid.grid.filter { it.contents?.isBasicOrb() == true }.random() ?: throw Exception("No valid tiles")

				addSpecial(tile.contents!!, special)

				return true
			})

			debugConsole.register("timemultiplier", "", fun(args, console): Boolean {
				timeMultiplier = args[0].toFloat()

				return true
			})

			debugConsole.register("setstat", "", fun (args, console): Boolean {

				val stat = Statistic.valueOf(args[0].toUpperCase())
				val value = args[1].toFloat()

				level.player.statistics[stat] = value

				console.write("$stat set to $value")

				return true
			})

			debugConsole.register("god", "", fun(args, console): Boolean
			{
				Global.godMode = !Global.godMode
				console.write("God Mode: ${Global.godMode}")

				return true
			})
		}
	}

	// ----------------------------------------------------------------------
	fun updateLevel(level: Level, player: Player)
	{
		if (!created)
		{
			baseCreate()
			created = true
		}

		//this.player = player
		this.level = level

		val gridWidget = GridWidget(level.grid)
		grid = gridWidget

		Global.player.isInBerserkRange = false
		Global.player.levelbuffs.clear()
		Global.player.leveldebuffs.clear()

		val powerBar = PowerBar()

		defeatTable.clear()
		for (defeat in level.defeatConditions)
		{
			val table = defeat.createTable(level.grid)
			defeatTable.add(table)
			defeatTable.row()
		}

		victoryTable.clear()
		for (victory in level.victoryConditions)
		{
			val table = victory.createTable(level.grid)
			victoryTable.add(table)
			victoryTable.row()
		}

		val buffDebuffTable = Table()
		buffTable.clear()
		debuffTable.clear()

		buffDebuffTable.add(buffTable).grow()
		buffDebuffTable.row()
		buffDebuffTable.add(debuffTable).grow()

		val abilityTable = Table()
		for (slot in EquipmentSlot.Values)
		{
			val equip = player.equipment[slot]
			val ability = equip?.ability
			when
			{
				ability != null ->
				{
					val widget = AbilityWidget(equip, 64f, 64f, level.grid)
					abilityTable.add(widget).expand()

					if (ability.data.resetUsagesPerLevel)
					{
						ability.remainingUsages = ability.data.maxUsages
					}
				}
				equip != null ->
				{
					val sprite = SpriteWidget(equip.icon.copy(), 64f, 64f)
					sprite.color = Color.DARK_GRAY
					abilityTable.add(sprite).expand()
				}
				else -> abilityTable.add(SpriteWidget(emptySlot, 64f, 64f)).expand()
			}
		}

		mainTable.clear()
		//val table = mainTable

		val baseTable = Table()
		baseTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.8f, 0.8f, 0.8f, 1f))
		baseTable.add(victoryTable).width(Value.percentWidth(0.35f, baseTable)).growY()
		baseTable.add(Seperator(Statics.skin, true)).growY().expandX()
		baseTable.add(buffDebuffTable).width(Value.percentWidth(0.2f, baseTable)).growY()
		baseTable.add(Seperator(Statics.skin, true)).growY().expandX()
		baseTable.add(defeatTable).width(Value.percentWidth(0.35f, baseTable)).growY()

		val powerBarStack = Stack()
		powerBarStack.add(powerBar)

		refreshButton = TextButton(Localisation.getText("gridscreen.novalidmoves", "UI"), Statics.skin)
		refreshButton!!.isVisible = false
		refreshButton!!.addClickListener {
			if (level.grid.activeAbility == null && level.grid.noValidMoves)
			{
				level.grid.refill()
				powerBar.power = 0
			}
		}
		powerBarStack.add(refreshButton)

		ultimateButton = TextButton(Localisation.getText("gridscreen.fullpower", "UI"), Statics.skin)
		ultimateButton!!.isVisible = false
		ultimateButton!!.addClickListener {
			if (powerBar.power == powerBar.maxPower)
			{
				level.grid.refill()
				powerBar.power = 0
			}
		}
		powerBarStack.add(ultimateButton)

		launchButton = TextButton(Localisation.getText("gridscreen.launch", "UI"), Statics.skin)
		launchButton!!.isVisible = false
		launchButton!!.addClickListener {
			if (level.grid.activeAbility != null && level.grid.activeAbility!!.selectedTargets.size > 0)
			{
				level.grid.activateAbility()
			}
		}
		powerBarStack.add(launchButton)

		completeButton = TextButton(Localisation.getText("gridscreen.skipanimations", "UI"), Statics.skin)
		completeButton!!.isVisible = false
		completeButton!!.addClickListener {
			if (level.victoryConditions.all { it.isCompleted() })
			{
				Mote.clear()

				for (label in level.grid.match.messageList)
				{
					label.remove()
				}

				level.complete()
			}
		}
		powerBarStack.add(completeButton)

		val topTable = Table()
		topTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("GUI/BasePanel")).tint(Color(0.8f, 0.8f, 0.8f, 1f))
		topTable.add(abilityTable).pad(10f).growX()
		topTable.row()
		topTable.add(powerBarStack).height(25f).pad(10f).growX()

		val table = Table()
		table.background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(level.theme.data.backgroundTile))).tint(Color(0.5f, 0.5f, 0.5f, 1f))

		table.add(topTable).growX()
		table.row()
		table.add(gridWidget).grow()
		table.row()
		table.add(baseTable).growX()

		mainTable.add(table).grow()

		launchTutorial = Tutorial("AbilityButton")
		launchTutorial.addPopup(Localisation.getText("gridscreen.launchtutorial", "UI"), launchButton!!)

		val tutorial = Tutorial("GridScreen")
		tutorial.addDelay(1f)
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.1", "UI"), Rectangle(stage.width / 2f, stage.height / 2f, 0f, 0f))
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.2", "UI"), victoryTable)
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.3", "UI"), defeatTable)
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.4", "UI"), abilityTable)
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.5", "UI"), powerBar)
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.6", "UI"), gridWidget)
		tutorial.addPopup(Localisation.getText("gridscreen.tutorial.7", "UI"), gridWidget)
		tutorial.show()
	}

	// ----------------------------------------------------------------------
	fun updateBuffTable()
	{
		buffTable.clear()
		for (buff in Global.player.levelbuffs)
		{
			val card = buff.getCardSmall(true)
			card.setFacing(true, false)
			card.setSize(15f, 25f)
			buffTable.add(card).size(15f, 25f)
		}

		val berserk = Global.player.getStat(Statistic.BERSERK)
		if (Global.player.isInBerserkRange && berserk > 0)
		{
			val basicTable = Table()

			val icon = AssetManager.loadSprite("GUI/Buff")

			basicTable.add(SpriteWidget(icon, 64f, 64f)).grow()
			basicTable.row()

			val table = Table()
			table.defaults().growX()

			val titleStack = Stack()
			val iconTable = Table()
			iconTable.add(SpriteWidget(icon, 64f, 64f)).expandX().right().pad(5f)
			titleStack.add(iconTable)
			titleStack.add(Label(Localisation.getText("gridscreen.berserk", "UI"), Statics.skin, "cardtitle"))

			table.add(titleStack).growX()
			table.row()

			table.add(Seperator(Statics.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f)
			table.row()

			table.add(Seperator(Statics.skin, "horizontalcard")).pad(10f, 0f, 10f, 0f)
			table.row()

			table.add(Label(Localisation.getText("statistics", "UI"), Statics.skin, "cardtitle"))
			table.row()

			for (stat in Statistic.Values)
			{
				if (stat != Statistic.MATCHDAMAGE && stat != Statistic.ABILITYDAMAGE && stat != Statistic.POWERGAIN && stat != Statistic.PIERCE)
				{
					continue
				}

				val statVal = if (stat == Statistic.PIERCE) berserk * 0.5f else berserk

				val statTable = Table()
				statTable.add(Label(stat.niceName + ": ", Statics.skin, "card")).expandX().left()
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
					val diffLabel = Label("+$diff", Statics.skin, "cardwhite")
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

			val card = CardWidget(basicTable, table, icon.currentTexture, this)
			card.setFacing(true, false)
			card.setSize(15f, 25f)
			buffTable.add(card).size(15f, 25f)
		}

		debuffTable.clear()
		for (debuff in Global.player.leveldebuffs)
		{
			val card = debuff.getCardSmall(false)
			card.setFacing(true, false)
			card.setSize(15f, 25f)
			debuffTable.add(card).size(15f, 25f)
		}
	}

	// ----------------------------------------------------------------------
	override fun keyDown(keycode: Int): Boolean
	{
		super.keyDown(keycode)

		return false
	}

	// ----------------------------------------------------------------------
	override fun doRender(delta: Float)
	{
		level.grid.update(delta)

		val ability = level.grid.activeAbility

		val canShowButtons =
			if (
				ability == null &&
				!level.grid.inTurn &&
				!level.grid.isUpdating &&
				(level.grid.noValidMoves || PowerBar.instance.power == PowerBar.instance.maxPower)
			)
				!level.grid.isUpdateBlocked()
			else
				false

		if (level.isVictory)
		{
			PowerBar.instance.isVisible = false
			launchButton!!.isVisible = false
			refreshButton!!.isVisible = false
			completeButton!!.isVisible = true
			ultimateButton!!.isVisible = false
		}
		else if (ability != null)
		{
			if (ability.selectedTargets.size != lastTargets)
			{
				idleTimer = 0f
				lastTargets = ability.selectedTargets.size
			}
			else if (idleTimer <= 2f)
			{
				idleTimer += delta
			}
			else if (idleTimer > 2f)
			{
				launchTutorial.show()
			}

			PowerBar.instance.isVisible = false
			refreshButton!!.isVisible = false
			ultimateButton!!.isVisible = false
			completeButton!!.isVisible = false
			launchButton!!.isVisible = true
			launchButton!!.color = if (ability.selectedTargets.size == 0) Color.DARK_GRAY else Color.WHITE
			launchButton!!.touchable = if (ability.selectedTargets.size == 0) Touchable.disabled else Touchable.enabled
			launchButton!!.setText(Localisation.getText("gridscreen.activate", "UI") + " (" + ability.selectedTargets.size + "/" + ability.data.targets + ")")
		}
		else if (level.grid.noValidMoves && canShowButtons)
		{
			PowerBar.instance.isVisible = false
			launchButton!!.isVisible = false
			ultimateButton!!.isVisible = false
			completeButton!!.isVisible = false
			refreshButton!!.isVisible = true
		}
		else if (PowerBar.instance.power == PowerBar.instance.maxPower && canShowButtons)
		{
			PowerBar.instance.isVisible = false
			launchButton!!.isVisible = false
			refreshButton!!.isVisible = false
			completeButton!!.isVisible = false
			ultimateButton!!.isVisible = true
		}
		else
		{
			PowerBar.instance.isVisible = true
			refreshButton!!.isVisible = false
			launchButton!!.isVisible = false
			ultimateButton!!.isVisible = false
			completeButton!!.isVisible = false
			idleTimer = 0f
			lastTargets = 0
		}
	}
	var idleTimer = 0f
	var lastTargets = 0
	lateinit var launchTutorial: Tutorial

	// ----------------------------------------------------------------------
	companion object
	{
		lateinit var instance: GridScreen
	}
}