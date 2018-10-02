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
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*

class CompletionConditionDie : AbstractCompletionCondition()
{
	lateinit var hpLabel: Label
	var maxHP: Int = 1
	var hp = 1
	var fractionalHp = 0f

	val blinkTable = Table()

	lateinit var table: Table

	override fun attachHandlers(grid: Grid)
	{
		maxHP = grid.level.player.getStat(Statistic.HEALTH).toInt()
		hp = maxHP

		grid.onAttacked += fun(c): Boolean {

			val sprite = c.sprite.copy()
			val dst = table.localToStageCoordinates(Vector2(Random.random() * table.width - table.width / 2f, Random.random() * table.height - table.height / 2f))
			val src = GridWidget.instance.pointToScreenspace(c)

			Mote(src, dst, sprite, GridWidget.instance.tileSize,
				 {
					 fun tryBlock(chance: Float): Boolean
					 {
						 return chance > 0f && Random.random.nextFloat() < chance
					 }

					 var blocked = false
					 var countered = false
					 if (tryBlock(Global.player.getStat(Statistic.REFLECT)))
					 {
						 blocked = true
						 countered = true
					 }
					 else if (tryBlock(Global.player.getStat(Statistic.AEGIS)))
					 {
						 blocked = true
					 }
					 else if (tryBlock(Global.player.getStat(Statistic.COUNTER)))
					 {
						 countered = true
					 }

					 if (blocked)
					 {
						 val pos = dst

						 val healSprite = AssetManager.loadParticleEffect("Block")
						 val actor = ParticleEffectActor(healSprite)
						 actor.setSize(48f, 48f)
						 actor.setPosition(pos.x, pos.y)
						 Global.stage.addActor(actor)
					 }
					 else
					 {
						 if (hp > 0) hp--

						 val pos = dst

						 val healSprite = AssetManager.loadParticleEffect("Hit")
						 val actor = ParticleEffectActor(healSprite)
						 actor.setSize(48f, 48f)
						 actor.setPosition(pos.x, pos.y)
						 Global.stage.addActor(actor)

						 hpLabel.setText("$hp/$maxHP")
						 updateBlink()
					 }

					 if (countered)
					 {
						 val src = dst
						 val target = grid.grid.filter { it.monster != null }.random()
						 if (target != null)
						 {
							 val dst = GridScreen.instance.grid!!.pointToScreenspace(target)
							 Mote(src, dst, sprite, GridWidget.instance.tileSize,
								  {
									  grid.pop(target, 0f, Global.player, Global.player.getStat(Statistic.MATCHDAMAGE), Global.player.getStat(Statistic.PIERCE))
								  }, animSpeed = 0.35f, leap = true)
						 }
					 }
				 }, animSpeed = 0.35f, leap = true)



			return false
		}

		grid.onTurn += {

			grid.level.player.isInBerserkRange = hp <= maxHP / 2

			fractionalHp += grid.level.player.getStat(Statistic.REGENERATION)
			while (fractionalHp > 1f)
			{
				fractionalHp -= 1f
				hp += 1

				val pos = hpLabel.localToStageCoordinates(Vector2(hpLabel.width/2f, hpLabel.height/2f))

				val healSprite = AssetManager.loadParticleEffect("Heal")
				val actor = ParticleEffectActor(healSprite)
				actor.setSize(48f, 48f)
				actor.setPosition(pos.x, pos.y)
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

		table = Table()
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