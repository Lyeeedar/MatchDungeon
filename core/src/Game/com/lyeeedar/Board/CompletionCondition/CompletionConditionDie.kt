package com.lyeeedar.Board.CompletionCondition

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.lyeeedar.Board.Grid
import com.lyeeedar.Board.spawnMote
import com.lyeeedar.Components.healable
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.sprite
import com.lyeeedar.Components.tile
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

		grid.onAttacked += fun(entity): HandlerAction {

			val sprite = entity.sprite()?.copy() ?: return HandlerAction.KeepAttached
			val pos = entity.pos() ?: return HandlerAction.KeepAttached
			val dst = table.localToStageCoordinates(Vector2(Random.random() * table.width, Random.random() * table.height))
			val moteDst = dst.cpy() - Vector2(GridWidget.instance.tileSize / 2f, GridWidget.instance.tileSize / 2f)
			val src = GridWidget.instance.pointToScreenspace(pos.position)

			// attack all friendlies
			for (tile in grid.friendlyTiles)
			{
				val friendly = tile.contents?.healable() ?: continue
				val newsprite = sprite.copy()
				val dst = GridWidget.instance.pointToScreenspace(tile)

				val duration = 0.35f
				spawnMote(src, dst, newsprite, GridWidget.instance.tileSize, {}, animSpeed = duration, leap = true)

				tile.addDelayedAction(
					{ tile ->
						val friendly = tile.contents?.healable()
						if (friendly != null)
						{
							friendly.hp--
						}
						val hit = grid.hitEffect.copy()
						tile.effects.add(hit)
					}, duration)
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
						if (grid.ran.nextFloat() < blockChance * remaining)
						{
							return true
						}
					}
					else
					{
						if (grid.ran.nextFloat() < blockChance)
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
			val animSpeed = 0.35f
			spawnMote(src, moteDst, sprite, GridWidget.instance.tileSize, {}, animSpeed = animSpeed, leap = true)

			pos.tile?.addDelayedAction(
				{
					if (blocked)
					{
						if (!Global.resolveInstantly)
						{
							val pos = dst

							val sprite = AssetManager.loadParticleEffect("Block")
							val actor = ParticleEffectActor(sprite.getParticleEffect())
							actor.setSize(48f, 48f)
							actor.setPosition(pos.x, pos.y)
							Statics.stage.addActor(actor)
						}
					}
					else
					{
						if (hp > 0) hp--

						if (!Global.resolveInstantly)
						{
							val pos = dst

							val sprite = AssetManager.loadParticleEffect("Hit")
							val actor = ParticleEffectActor(sprite.getParticleEffect())
							actor.setSize(48f, 48f)
							actor.setPosition(pos.x, pos.y)
							Statics.stage.addActor(actor)

							hpLabel.setText("$hp/$maxHP")
							updateBlink()
						}
					}

					if (countered)
					{
						val src = dst
						val target = grid.monsterTiles.random()
						if (target != null)
						{
							val dst = if (Global.resolveInstantly) Vector2() else GridScreen.instance.grid!!.pointToScreenspace(target)
							spawnMote(src, dst, sprite, GridWidget.instance.tileSize, {}, animSpeed = 0.35f, leap = true)
							target.addDelayedAction(
								{ target ->
									grid.pop(target, 0f, Global.player, Global.player.getStat(Statistic.MATCHDAMAGE), Global.player.getStat(Statistic.PIERCE))
								}, 0.35f)
						}
					}
				}, animSpeed)

			return HandlerAction.KeepAttached
		}

		grid.onTurn += {

			grid.level.player.isInBerserkRange = hp <= maxHP / 2

			fractionalHp += grid.level.player.getStat(Statistic.REGENERATION)

			updateFractionalHp()

			HandlerAction.KeepAttached
		}

		if (!Global.resolveInstantly)
		{
			Future.call(
				{
					val tutorial = Tutorial("Die")
					tutorial.addPopup(Localisation.getText("completioncondition.die.tutorial", "UI"), hpLabel)
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
	}

	fun updateBlink()
	{
		if (Global.resolveInstantly) return

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

			if (!Global.resolveInstantly)
			{
				val pos = hpLabel.localToStageCoordinates(Vector2(hpLabel.width / 2f, hpLabel.height / 2f))

				val healSprite = AssetManager.loadParticleEffect("Heal")
				healSprite.colour = Colour.GREEN
				val actor = ParticleEffectActor(healSprite.getParticleEffect())
				actor.setSize(48f, 48f)
				actor.setPosition(pos.x, pos.y)
				Statics.stage.addActor(actor)
			}

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
			if (!Global.resolveInstantly)
			{
				val pos = hpLabel.localToStageCoordinates(Vector2(hpLabel.width / 2f, hpLabel.height / 2f))

				val healSprite = AssetManager.loadParticleEffect("Heal")
				healSprite.colour = Colour.RED
				healSprite.flipY = true
				val actor = ParticleEffectActor(healSprite.getParticleEffect())
				actor.setSize(48f, 48f)
				actor.setPosition(pos.x, pos.y)
				Statics.stage.addActor(actor)
			}
		}

		while (fractionalHp < -1f)
		{
			fractionalHp += 1f
			hp -= 1

			hpLabel.setText("$hp / $maxHP")
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

		table.add(Label(Localisation.getText("completioncondition.die.description", "UI"), Statics.skin))

		return table
	}
}