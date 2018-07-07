package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.lyeeedar.Board.Grid
import com.lyeeedar.Game.Ability.Ability
import com.lyeeedar.Util.AssetManager

/**
 * Created by Philip on 20-Jul-16.
 */

class AbilityWidget(val ability: Ability, val w: Float, val h: Float, val grid: Grid) : Table()
{
	val empty = AssetManager.loadSprite("GUI/power_empty")
	val full = AssetManager.loadSprite("GUI/power_full")

	val padding = 3

	val widget = SpriteWidget(ability.icon, w, h)

	init
	{
		add(widget).expand().fill()

		PowerBar.instance.powerChanged += {
			updateEnabled()

			false
		}
		grid.onTurn += {
			updateEnabled()

			false
		}

		updateEnabled()

		addListener(object: ClickListener()
		{
			override fun clicked(event: InputEvent?, x: Float, y: Float)
			{
				if (PowerBar.instance.pips >= ability.cost && ability.getValidTargets(grid).size > 0)
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
		if (PowerBar.instance.pips >= ability.cost && ability.getValidTargets(grid).size > 0)
		{
			colour = Color.WHITE
		}
		else
		{
			colour = Color.DARK_GRAY
		}
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		if (grid.activeAbility == ability)
		{
			widget.color = Color.GOLD
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
	}
}
