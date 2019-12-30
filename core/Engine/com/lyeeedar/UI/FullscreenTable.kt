package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Event0Arg
import com.lyeeedar.Util.Statics

/**
 * Created by Philip on 02-Aug-16.
 */

open class FullscreenTable(opacity: Float = 0.4f) : Table()
{
	val onClosed = Event0Arg()

	var closeButton: Button? = null

	init
	{
		openCount++

		background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, opacity))
		touchable = Touchable.enabled
		setFillParent(true)

		Statics.stage.addActor(this)
	}

	override fun remove(): Boolean
	{
		openCount--

		onClosed()
		closeButton?.remove()
		return super.remove()
	}

	companion object
	{
		var openCount = 0

		fun createCloseable(content: Table): Table
		{
			val xPad = 15f * (openCount+1)

			val contentTable = Table()
			contentTable.add(content).grow()

			val table = FullscreenTable()
			table.add(contentTable).grow().pad(15f, xPad, 15f, xPad)

			contentTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))

			val closeButton = Button(Statics.skin, "close")
			closeButton.setSize(24f, 24f)
			closeButton.addClickListener {
				table.remove()
			}
			closeButton.setPosition(Statics.stage.width - 35 - xPad, Statics.stage.height - 50)

			Statics.stage.addActor(closeButton)
			table.closeButton = closeButton

			return table
		}

		fun createCard(content: Table, point: Vector2)
		{
			val cardWidget = CardWidget(content, content, AssetManager.loadTextureRegion("white")!!, null)
			cardWidget.setSize(24f, 24f)
			cardWidget.setPosition(point.x, point.y)

			Statics.stage.addActor(cardWidget)

			cardWidget.focus()

			cardWidget.collapseFun = {
				cardWidget.remove()
			}
		}
	}
}