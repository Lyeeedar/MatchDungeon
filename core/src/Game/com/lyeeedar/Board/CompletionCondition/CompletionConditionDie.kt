package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.Mote
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Statistic
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.ParticleEffectActor
import com.lyeeedar.UI.SpriteWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.math.minus

class CompletionConditionDie : AbstractCompletionCondition()
{
	lateinit var hpLabel: Label
	var maxHP: Int = 1
	var hp = 1
	var fractionalHp = 0f

	var godMode = false

	val blinkTable = Table()

	lateinit var table: Table

	override fun attachHandlers(grid: Grid)
	{
		maxHP = grid.level.player.getStat(Statistic.HEALTH).toInt()
		hp = maxHP

		grid.onAttacked += fun(c): Boolean {

			val sprite = c.sprite.copy()
			val dst = table.localToStageCoordinates(Vector2(Random.random() * table.width, Random.random() * table.height))
			val moteDst = dst.cpy() - Vector2(GridWidget.instance.tileSize / 2f, GridWidget.instance.tileSize / 2f)
			val src = GridWidget.instance.pointToScreenspace(c)

			// attack all friendlies
			for (tile in grid.grid)
			{
				val friendly = tile.friendly ?: continue
				val sprite = c.sprite.copy()
				val dst = GridWidget.instance.pointToScreenspace(tile)

				Mote(src, dst, sprite, GridWidget.instance.tileSize,
					 {
						grid.damage(tile, friendly, 0f, friendly)

					 }, animSpeed = 0.35f, leap = true)
			}

			// calculate block and counter
			fun tryBlock(chance: Float): Boolean
			{
				val blockChance = 0.2f

				var remaining = chance
				while (remaining > 0)
				{
					if (remaining < 1f)
					{
						if (Random.random.nextFloat() < blockChance * remaining)
						{
							return true
						}
					}
					else
					{
						if (Random.random.nextFloat() < blockChance)
						{
							return true
						}
					}

					remaining -= 1f
				}

				return false
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

			if (godMode)
			{
				blocked = true
			}

			// animate player attack
			Mote(src, moteDst, sprite, GridWidget.instance.tileSize,
				 {
					 if (blocked)
					 {
						 val pos = dst

						 val sprite = AssetManager.loadParticleEffect("Block")
						 val actor = ParticleEffectActor(sprite.getParticleEffect())
						 actor.setSize(48f, 48f)
						 actor.setPosition(pos.x, pos.y)
						 Statics.stage.addActor(actor)
					 }
					 else
					 {
						 if (hp > 0) hp--

						 val pos = dst

						 val sprite = AssetManager.loadParticleEffect("Hit")
						 val actor = ParticleEffectActor(sprite.getParticleEffect())
						 actor.setSize(48f, 48f)
						 actor.setPosition(pos.x, pos.y)
						 Statics.stage.addActor(actor)

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

			updateFractionalHp()

			false
		}

		Future.call(
				{
					val tutorial = Tutorial("Die")
					tutorial.addPopup("This is your remaining health. Attacks will reduce it, and when it reaches 0 you will fail the level.", hpLabel)
					tutorial.show()
				}, 0.5f)

		if (!Statics.release)
		{
			Future.call({
							GridScreen.instance.debugConsole.reregister("god", "", fun(args, console): Boolean
							{

								godMode = !godMode
								console.write("God Mode: $godMode")

								return true
							})
						}, 1f)
		}
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

	fun updateFractionalHp()
	{
		// do regen
		while (fractionalHp > 1f)
		{
			fractionalHp -= 1f
			hp += 1

			val pos = hpLabel.localToStageCoordinates(Vector2(hpLabel.width/2f, hpLabel.height/2f))

			val healSprite = AssetManager.loadParticleEffect("Heal")
			healSprite.colour = Colour.GREEN
			val actor = ParticleEffectActor(healSprite.getParticleEffect())
			actor.setSize(48f, 48f)
			actor.setPosition(pos.x, pos.y)
			Statics.stage.addActor(actor)

			if (hp > maxHP)
			{
				hp = maxHP
			}

			hpLabel.setText("$hp/$maxHP")
			updateBlink()
		}

		// do degen
		if (Global.player.getStat(Statistic.REGENERATION) < 0f)
		{
			val pos = hpLabel.localToStageCoordinates(Vector2(hpLabel.width/2f, hpLabel.height/2f))

			val healSprite = AssetManager.loadParticleEffect("Heal")
			healSprite.colour = Colour.RED
			healSprite.flipY = true
			val actor = ParticleEffectActor(healSprite.getParticleEffect())
			actor.setSize(48f, 48f)
			actor.setPosition(pos.x, pos.y)
			Statics.stage.addActor(actor)
		}

		while (fractionalHp < -1f)
		{
			fractionalHp += 1f
			hp -= 1

			hpLabel.setText("$hp/$maxHP")
			updateBlink()
		}
	}

	override fun isCompleted(): Boolean = hp <= 0

	override fun parse(xml: XmlData)
	{

	}

	override fun createTable(grid: Grid): Table
	{
		hpLabel = Label("$hp/$maxHP", Statics.skin)

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

		table.add(Label("Don't let your hp reach 0.", Statics.skin))

		return table
	}
}