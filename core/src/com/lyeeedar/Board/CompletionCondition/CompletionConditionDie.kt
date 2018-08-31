package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Mote
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffectActor
import com.lyeeedar.Statistic
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.XmlData

class CompletionConditionDie : AbstractCompletionCondition()
{
	var regenAccumulator = 0f

	lateinit var hpLabel: Label
	var maxHP: Int = 1
	var hp = 1

	val blinkTable = Table()

	override fun attachHandlers(grid: Grid)
	{
		maxHP = grid.level.player.getStat(Statistic.HEALTH).toInt()
		hp = maxHP

		grid.onAttacked += fun(c): Boolean {

			val sprite = c.sprite.copy()
			val dst = hpLabel.localToStageCoordinates(Vector2())
			val src = GridWidget.instance.pointToScreenspace(c)

			Mote(src, dst, sprite, GridWidget.instance.tileSize,
				 {
					 if (hp > 0) hp--
					 hpLabel.setText("$hp/$maxHP")
					 updateBlink()
				 })



			return false
		}

		grid.onTurn += {

			grid.level.player.isInBerserkRange = hp <= maxHP / 2

			regenAccumulator += grid.level.player.getStat(Statistic.REGENERATION)
			while (regenAccumulator > 1f)
			{
				regenAccumulator -= 1f
				hp += 1

				val pos = hpLabel.localToStageCoordinates(Vector2(hpLabel.width/2f, hpLabel.height/2f))

				val healSprite = AssetManager.loadParticleEffect("Heal")
				val actor = ParticleEffectActor(healSprite, 32f, pos)
				Global.stage.addActor(actor)

				if (hp > maxHP)
				{
					hp = maxHP
				}

				hpLabel.setText("$hp/$maxHP")
				updateBlink()
			}

			false
		}

		Future.call(
				{
					val tutorial = Tutorial("Die")
					tutorial.addPopup("This is your remaining health. Attacks will reduce it, and when it reaches 0 you will fail the level.", hpLabel)
					tutorial.show()
				}, 0.5f)
	}

	fun updateBlink()
	{
		if (hp <= maxHP * 0.25f)
		{
			if (blinkTable.children.size == 0)
			{
				val blinkSprite = AssetManager.loadSprite("Particle/glow")
				blinkSprite.colour = Colour.RED.copy().a(0.5f)
				blinkSprite.animation = ExpandAnimation.obtain().set(1f, 0.5f, 2f, false, true)
				val actor = SpriteWidget(blinkSprite, 32f, 32f)
				blinkTable.add(actor).grow()
			}
		}
		else
		{
			blinkTable.clear()
		}
	}

	override fun isCompleted(): Boolean = hp <= 0

	override fun parse(xml: XmlData)
	{

	}

	override fun createTable(grid: Grid): Table
	{
		hpLabel = Label("$hp/$maxHP", Global.skin)

		val stack = Stack()
		stack.add(blinkTable)
		stack.add(hpLabel)

		val table = Table()
		table.defaults().pad(10f)
		table.add(stack).grow()

		return table
	}

	override fun getDescription(grid: Grid): Table
	{
		val table = Table()

		table.add(Label("Don't let your hp reach 0.", Global.skin))

		return table
	}
}