package com.lyeeedar.Screens

import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable
import com.lyeeedar.Board.Level
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.PowerBar
import com.lyeeedar.UI.TutorialPopup
import com.lyeeedar.Util.AssetManager
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
	var grid: GridWidget? = null
	//lateinit var player: Player
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
	}

	// ----------------------------------------------------------------------
	fun updateLevel(level: Level)
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
//		for (ability in player.abilities)
//		{
//			if (ability != null)
//			{
//				val widget = AbilityWidget(ability, 64f, 64f, level.grid)
//				abilityTable.add(widget).expand()
//			}
//			else
//			{
//				abilityTable.add(SpriteWidget(emptySlot, 64f, 64f)).expand()
//			}
//		}

		mainTable.clear()
		//val table = mainTable

		val table = table {
			defaults().pad(10f).growX()
			background = TiledDrawable(TextureRegionDrawable(level.theme.floor.sprite!!.currentTexture)).tint(Color.DARK_GRAY)

			add(abilityTable)
			row()
			stack { cell -> cell.height(25f)
				add(powerBar)
				refreshButton = textButton("No Valid Moves. Refresh?", "default", Global.skin) {
					isVisible = false
					onClick { inputEvent: InputEvent, kTextButton: KTextButton ->
						if (level.grid.activeAbility == null && level.grid.noValidMoves)
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
	}

	override fun keyDown(keycode: Int): Boolean
	{
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

		if (ability != null)
		{
			if (ability.selectedTargets.size != lastTargets)
			{
				idleTimer = 0f
				lastTargets = ability.selectedTargets.size
			}
			else if (idleTimer <= 2f && popup == null)
			{
				idleTimer += delta
			}
			else if (idleTimer > 2f && popup == null && !squashed)
			{
				popup = TutorialPopup("Click activate to use your ability on the selected tiles.", launchButton!!.localToStageCoordinates(Vector2()), "AbilityButton")
			}

			PowerBar.instance.isVisible = false
			refreshButton!!.isVisible = false
			launchButton!!.isVisible = true
			launchButton!!.color = if (ability.selectedTargets.size == 0) Color.DARK_GRAY else Color.WHITE
			launchButton!!.touchable = if (ability.selectedTargets.size == 0) Touchable.disabled else Touchable.enabled
			launchButton!!.setText("Activate (" + ability.selectedTargets.size + "/" + ability.targets + ")")
		}
		else if (level.grid.noValidMoves)
		{
			PowerBar.instance.isVisible = false
			launchButton!!.isVisible = false
			refreshButton!!.isVisible = true
		}
		else
		{
			PowerBar.instance.isVisible = true
			refreshButton!!.isVisible = false
			launchButton!!.isVisible = false
			idleTimer = 0f
			lastTargets = 0
		}
	}
	var squashed = false
	var idleTimer = 0f
	var lastTargets = 0
	var popup: TutorialPopup? = null

	// ----------------------------------------------------------------------
	companion object
	{
		lateinit var instance: GridScreen
	}
}