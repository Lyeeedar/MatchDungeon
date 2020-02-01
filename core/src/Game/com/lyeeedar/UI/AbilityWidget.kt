package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.lyeeedar.Board.Grid
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.HandlerAction
import com.lyeeedar.Util.Localisation
import com.lyeeedar.Util.Statics

class AbilityWidget(val equipment: Equipment, val w: Float, val h: Float, val grid: Grid) : Table()
{
	val empty = AssetManager.loadSprite("GUI/power_empty")
	val full = AssetManager.loadSprite("GUI/power_full")
	val border = AssetManager.loadTextureRegion("GUI/border")

	val padding = 3

	val widget = SpriteWidget(equipment.icon.copy(), w, h)

	val ability = equipment.ability!!

	val usagesLabel = Label("", Statics.skin)

	init
	{
		val stack = Stack()
		stack.add(widget)

		val infoButton = Button(Statics.skin, "info")
		infoButton.setSize(16f, 16f)
		infoButton.addClickListener {
			FullscreenTable.createCard(ability.getCard(), infoButton.localToStageCoordinates(Vector2()))
		}
		val infoButtonTable = Table()
		infoButtonTable.add(infoButton).size(16f).expand().top().right().pad(5f)

		stack.add(usagesLabel)

		stack.add(infoButtonTable)

		add(stack).grow()

		PowerBar.instance.powerChanged += {
			updateEnabled()

			HandlerAction.KeepAttached
		}
		grid.onTurn += {
			updateEnabled()

			HandlerAction.KeepAttached
		}

		updateEnabled()

		addListener(object: ClickListener()
		{
			override fun clicked(event: InputEvent?, x: Float, y: Float)
			{
				if (PowerBar.instance.pips >= ability.cost && ability.hasValidTargets(grid))
				{
					if (grid.activeAbility == ability)
					{
						grid.activeAbility = null
						ability.selectedTargets.clear()
					}
					else
					{
						grid.activeAbility = ability
					}
				}
			}
		})

		touchable = Touchable.enabled
	}

	var colour: Color = Color.DARK_GRAY
	fun updateEnabled()
	{
		if (PowerBar.instance.pips >= ability.cost && ability.hasValidTargets(grid))
		{
			colour = Color.WHITE
		}
		else
		{
			colour = Color.DARK_GRAY
		}

		if (ability.maxUsages > 0 && !ability.resetUsagesPerLevel)
		{
			usagesLabel.setText("" + ability.remainingUsages)
		}
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		if (grid.activeAbility == ability)
		{
			widget.color = Color.GOLD

			batch?.color = Color.GOLD
			batch?.draw(border, x, y, width, height)
		}
		else
		{
			widget.color = colour
		}

		super.draw(batch, parentAlpha)

		val pipSize = (width - (ability.cost+1) * padding) / ability.cost
		val filledPips = PowerBar.instance.pips

		batch!!.color = Color.WHITE

		for (i in 1..ability.cost)
		{
			val sprite = if (i <= filledPips) full else empty

			sprite.render(batch as SpriteBatch, x + padding * i + (i-1) * pipSize, y, pipSize, 10f)
		}

		if (!Statics.settings.get("Ability", false))
		{
			val tutorial = Tutorial("Ability")
			tutorial.addPopup(Localisation.getText("ability.tutorial.1", "UI"), this)
			tutorial.addPopup(Localisation.getText("ability.tutorial.2", "UI"), Rectangle(x, y, width, 10f))
			tutorial.addPopup(Localisation.getText("ability.tutorial.3", "UI"), this)
			tutorial.show()
		}
	}
}
