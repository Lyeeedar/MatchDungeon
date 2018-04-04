package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.lyeeedar.Game.Save
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager

class UpgradesWidget(skin: Skin) : Table(skin)
{
	val emptySlot = AssetManager.loadSprite("Icons/Empty")

	init
	{
		background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))

		rebuild()
	}

	private fun rebuild()
	{
		clear()

		add(Label("Upgrades", skin, "title")).colspan(3)
		row()
		add(Seperator(skin)).colspan(3).growX().pad(5f)
		row()

		add(Table())
		add("Gold:").expandX().right()
		add(Global.player.gold.toString()).pad(10f)
		row()

		addUpgrade("Max HP:", 100, {Global.player.maxhpStat}, {Global.player.maxhpStat = it})
		addUpgrade("Power Gain:", 100, {Global.player.powerGainStat}, {Global.player.powerGainStat = it})
		addUpgrade("Match Dam:", 1000, {Global.player.attackDamStat}, {Global.player.attackDamStat = it})
		addUpgrade("Special Dam:", 500, {Global.player.abilityDamStat}, {Global.player.abilityDamStat = it})

		add(Label("Abilities", skin, "title")).colspan(3)
		row()
		add(Seperator(skin)).colspan(3).growX().pad(5f)
		row()

		val abilityTreeButton = TextButton("Ability Tree", skin)
		abilityTreeButton.addClickListener {
			val widget = Table()
			val closeButton = Button(Global.skin, "close")
			closeButton.setSize(24f, 24f)

			val skills = UnlockTreeWidget(Global.player.abilityTree)
			val scroll = ScrollPane(skills)
			scroll.setFlingTime(0f)
			scroll.setOverscroll(false, false)
			widget.add(scroll).expand().fill()

			widget.setFillParent(true)
			Global.stage.addActor(widget)

			scroll.layout()
			scroll.scrollTo(skills.prefWidth/3, 0f, 1f, 1f, true, true)
			scroll.act(1f)

			closeButton.addClickListener({ widget.remove(); closeButton.remove(); rebuild() })
			Global.stage.addActor(closeButton)
			closeButton.setPosition(Global.stage.width - 50, Global.stage.height - 50)
		}
		add(abilityTreeButton).colspan(3).pad(5f)
		row()

		val abilityTable = Table()

		for (i in 0..3)
		{
			val ability = Global.player.abilities[i]
			var sprite: Sprite

			if (ability != null)
			{
				sprite = ability.icon.copy()
			}
			else
			{
				sprite = emptySlot.copy()
			}

			val widget = SpriteWidget(sprite, 48f, 48f)
			abilityTable.add(widget).expandX()

			widget.addClickListener {
				val abName = ability?.upgrades ?: ability?.name
				UnlockablesList(abName, Global.player.abilityTree, { it -> Global.player.abilities[i] = Global.player.getAbility(it); Save.save(); rebuild()}, { !Global.player.abilities.contains(it) })
			}
		}

		add(abilityTable).colspan(3).growX().pad(10f)
		row()
	}

	private fun addUpgrade(name: String, baseCost: Int, getFun: () -> Int, setFun: (value: Int) -> Unit)
	{
		add(Label(name, skin)).width(120f)

		add(UpgradeLevelWidget(getFun)).growX()

		val originalVal = getFun()
		val cost = upgradeCost(baseCost, originalVal)

		if (Global.player.gold < cost)
		{
			add(Label(cost.toString(), skin)).width(100f).height(32f).pad(5f)
		}
		else
		{
			val button = TextButton(cost.toString(), skin)
			button.addClickListener {
				Global.player.gold -= cost
				setFun(originalVal+1)
				Save.save()
				rebuild() }
			add(button).width(100f).height(32f).pad(5f)
		}

		row()
	}

	private fun upgradeCost(base: Int, level: Int): Int
	{
		var output = base.toFloat()
		for (i in 0 until level)
		{
			output *= 1.5f
		}
		return output.toInt()
	}
}

class UpgradeLevelWidget(val getFun: () -> Int) : Widget()
{
	val full: TextureRegion = AssetManager.loadTextureRegion("GUI/health_full")!!
	val empty: TextureRegion = AssetManager.loadTextureRegion("GUI/health_empty")!!

	override fun getPrefHeight(): Float = 24f

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val solidSpaceRatio = 0.12f // 20% free space
		val space = width
		val spacePerPip = space / 10f
		val spacing = spacePerPip * solidSpaceRatio
		val solid = spacePerPip - spacing

		val currentVal = getFun()
		for (i in 0 until 10)
		{
			val sprite = if(i < currentVal) full else empty
			batch.draw(sprite, x + i * spacePerPip, y, solid, height)
		}
	}
}
