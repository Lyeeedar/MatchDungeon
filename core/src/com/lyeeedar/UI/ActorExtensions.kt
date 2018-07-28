package com.lyeeedar.UI

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.lyeeedar.Global

fun Actor.getBounds(): Rectangle
{
	val pos = localToStageCoordinates(Vector2())
	return Rectangle(pos.x, pos.y, this.width, this.height)
}

fun Actor.ensureOnScreen(pad: Float = 0f)
{
	if (width > stage.width - pad*2f)
	{
		width = stage.width - pad*2f
	}

	if (height > stage.height - pad*2f)
	{
		height = stage.height - pad*2f
	}

	// Fit within stage

	if (x < pad)
	{
		x = pad
	}
	else if (x + width > stage.width - pad)
	{
		x = stage.width - width - pad
	}

	if (y < pad)
	{
		y = pad
	}
	else if (y + height > stage.height - pad)
	{
		y = stage.height - height - pad
	}

	setPosition(x, y)
}

fun Actor.addClickListener(func: () -> Unit)
{
	this.touchable = Touchable.enabled
	this.addListener(object : ClickListener() {
		override fun clicked(event: InputEvent?, x: Float, y: Float)
		{
			event?.handle()
			super.clicked(event, x, y)
			func()
		}
	})
}

fun Actor.addClickListenerFull(func: (InputEvent?, Float, Float) -> Unit)
{
	this.touchable = Touchable.enabled
	this.addListener(object : ClickListener() {
		override fun clicked(event: InputEvent?, x: Float, y: Float)
		{
			event?.handle()
			super.clicked(event, x, y)
			func(event, x, y)
		}
	})
}

fun Actor.addTapToolTip(content: String)
{
	this.addClickListenerFull { event, x, y ->

		val table = Table()
		val label = Label(content, Global.skin)
		label.setWrap(true)
		table.add(label).grow().pad(10f).prefWidth(200f).center()

		val tooltip = Tooltip(table, Global.skin, Global.stage)

		val fullscreenTable = Table()
		fullscreenTable.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
		fullscreenTable.setFillParent(true)
		fullscreenTable.addClickListener {
			tooltip.remove()
			Tooltip.openTooltip = null
			fullscreenTable.remove()
		}
		Global.stage.addActor(fullscreenTable)

		tooltip.toFront()
		tooltip.show(event, x, y)
	}
}

fun Actor.addToolTip(title: String, body: String, stage: Stage)
{
	val titleLabel = Label(title, Global.skin, "title")
	val bodyLabel = Label(body, Global.skin)

	val table = Table()
	table.add(titleLabel).expandX().center()
	table.row()
	table.add(bodyLabel).expand().fill()

	val tooltip = Tooltip(table, Global.skin, stage)

	this.addListener(TooltipListener(tooltip))
}