package com.lyeeedar.Screens

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Board.*
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTurns
import com.lyeeedar.EquipmentSlot
import com.lyeeedar.Game.Player
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.random
import ktx.actors.onClick
import ktx.scene2d.KTextButton
import ktx.scene2d.stack
import ktx.scene2d.table
import ktx.scene2d.textButton

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

	// ----------------------------------------------------------------------
	init
	{
		instance = this
	}

	// ----------------------------------------------------------------------
	override fun create()
	{
		if (!Global.release)
		{
			debugConsole.register("complete", "", { args, console ->
				level.victoryAction.invoke()

				true
			})

			debugConsole.register("fail", "", { args, console ->
				level.defeatAction.invoke()

				true
			})

			debugConsole.register("setturns", "", fun(args, console): Boolean
			{
				val condition = level.defeatConditions.filter { it is CompletionConditionTurns }.firstOrNull()

				if (condition == null || condition !is CompletionConditionTurns)
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
				val die = level.defeatConditions.filter { it is CompletionConditionDie }.firstOrNull()

				if (die == null || die !is CompletionConditionDie)
				{
					console.error("No die defeat condition!")
					return false
				}

				die.hp = 0

				return true
			})

			debugConsole.register("maxpower", "", { args, console ->
				PowerBar.instance.power = PowerBar.instance.maxPower

				true
			})

			debugConsole.register("spawn", "", fun(args, console): Boolean {
				if (args[0] == "match5")
				{
					val tile = level.grid.grid.filter { it.orb != null }.random()!!
					tile.orb!!.special = Match5(tile.orb!!)
				}
				else if (args[0] == "match4")
				{
					if (args[1] == "hori")
					{
						val tile = level.grid.grid.filter { it.orb != null }.random()!!
						tile.orb!!.special = Horizontal4(tile.orb!!)
					}
					else if (args[1] == "vert")
					{
						val tile = level.grid.grid.filter { it.orb != null }.random()!!
						tile.orb!!.special = Vertical4(tile.orb!!)
					}
					else
					{
						return false
					}
				}
				else if (args[0] == "dualmatch")
				{
					val tile = level.grid.grid.filter { it.orb != null }.random()!!
					tile.orb!!.special = DualMatch(tile.orb!!)
				}
				else
				{
					return false
				}

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

		val powerBar = PowerBar()

		val victoryTable = Table()

		defeatTable.clear()
		for (defeat in level.defeatConditions)
		{
			val table = defeat.createTable(level.grid)
			defeatTable.add(table)
			defeatTable.row()
		}

		for (victory in level.victoryConditions)
		{
			val table = victory.createTable(level.grid)
			victoryTable.add(table)
			victoryTable.row()
		}

		defeatTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))
		victoryTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))

		val abilityTable = Table()
		for (slot in EquipmentSlot.Values)
		{
			val equip = player.getEquipment(slot)
			if (equip?.ability != null)
			{
				val widget = AbilityWidget(equip, 64f, 64f, level.grid)
				abilityTable.add(widget).expand()
			}
			else if (equip != null)
			{
				val sprite = SpriteWidget(equip.icon.copy(), 64f, 64f)
				sprite.color = Color.DARK_GRAY
				abilityTable.add(sprite).expand()
			}
			else
			{
				abilityTable.add(SpriteWidget(emptySlot, 64f, 64f)).expand()
			}
		}

		mainTable.clear()
		//val table = mainTable

		val table = table {
			defaults().pad(10f).growX()
			background = TiledDrawable(TextureRegionDrawable(AssetManager.loadTextureRegion(level.theme.backgroundTile))).tint(Color.DARK_GRAY)

			add(abilityTable)
			row()
			stack { cell -> cell.height(25f)
				add(powerBar)

				refreshButton = textButton("No Valid Moves. Shuffle Grid?", "default", Global.skin) {
					isVisible = false
					onClick { inputEvent: InputEvent, kTextButton: KTextButton ->
						if (level.grid.activeAbility == null && level.grid.noValidMoves)
						{
							level.grid.refill()
							powerBar.power = 0
						}
					}
				}

				ultimateButton = textButton("Full Power! Shuffle Grid?", "default", Global.skin) {
					isVisible = false
					onClick { inputEvent: InputEvent, kTextButton: KTextButton ->
						if (powerBar.power == powerBar.maxPower)
						{
							level.grid.refill()
							powerBar.power = 0
						}
					}
				}

				launchButton = textButton("Launch", "default", Global.skin) {
					isVisible = false
					onClick { inputEvent: InputEvent, kTextButton: KTextButton ->
						if (level.grid.activeAbility != null && level.grid.activeAbility!!.selectedTargets.size > 0)
						{
							level.grid.activateAbility()
						}
					}
				}

				completeButton = textButton("Level complete! Skip animations?", "default", Global.skin) {
					isVisible = false
					onClick { inputEvent: InputEvent, kTextButton: KTextButton ->
						if (level.victoryConditions.all { it.isCompleted() })
						{
							Mote.clear()

							level.completed = true
							level.complete()
						}
					}
				}
			}
			row()
			add(gridWidget).grow()
			row()
			table {
				add(victoryTable).grow().left()


				add(defeatTable).grow().right()
			}
		}

		mainTable.add(table).grow()

		launchTutorial = Tutorial("AbilityButton")
		launchTutorial.addPopup("Click activate to use your ability on the selected tiles.", launchButton!!)

		val tutorial = Tutorial("GridScreen")
		tutorial.addDelay(1f)
		tutorial.addPopup("This is the match 3 board.", Rectangle(Global.stage.width / 2f, Global.stage.height / 2f, 0f, 0f))
		tutorial.addPopup("These are your victory conditions", victoryTable)
		tutorial.addPopup("These are your failure conditions", defeatTable)
		tutorial.addPopup("These are your abilities, provided by your equipment.", abilityTable)
		tutorial.addPopup("This is the power bar. You use the power collected here to use abilities, or when it is full you can discharge it all to shuffle the board.", powerBar)
		tutorial.addPopup("This area contains the orbs you match. Make rows of 3 orbs of the same colour to match them.", gridWidget)
		tutorial.addPopup("Make rows of 4 or 5 to spawn a special orb, which when matched has a special effect.", gridWidget)
		tutorial.show()
	}

	// ----------------------------------------------------------------------
	override fun keyDown(keycode: Int): Boolean
	{
		super.keyDown(keycode)

		if (keycode == Input.Keys.D)
		{
			grid!!.ground.debugDraw = !grid!!.ground.debugDraw
		}
		else if (keycode == Input.Keys.UP)
		{
			if (grid!!.ground.debugDraw)
			{
				grid!!.ground.debugDrawSpeed++
			}
		}
		else if (keycode == Input.Keys.DOWN)
		{
			if (grid!!.ground.debugDraw)
			{
				grid!!.ground.debugDrawSpeed--
			}
		}

		return false
	}

	// ----------------------------------------------------------------------
	override fun doRender(delta: Float)
	{
		level.update(delta)

		val ability = level.grid.activeAbility

		val canShowButtons = if (ability == null && !level.grid.inTurn && (level.grid.noValidMoves || PowerBar.instance.power == PowerBar.instance.maxPower)) !level.grid.hasAnim() else false

		if (level.victoryConditions.all { it.isCompleted() })
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
			launchButton!!.setText("Activate (" + ability.selectedTargets.size + "/" + ability.targets + ")")
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