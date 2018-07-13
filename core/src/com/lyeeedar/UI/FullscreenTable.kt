package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Button
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Event0Arg

/**
 * Created by Philip on 02-Aug-16.
 */

open class FullscreenTable() : Table()
{
	val onClosed = Event0Arg()

	init
	{
		background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.4f))
		touchable = Touchable.enabled
		setFillParent(true)

		Global.stage.addActor(this)
	}

	override fun remove(): Boolean
	{
		onClosed()
		return super.remove()
	}

	companion object
	{
		var openCount = 0

		fun createCloseable(content: Table)
		{
			val xPad = 15f * (openCount+1)

			val contentTable = Table()
			contentTable.add(content).grow()

			val table = FullscreenTable()
			table.add(contentTable).grow().pad(15f, xPad, 15f, xPad)

			contentTable.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))

			val closeButton = Button(Global.skin, "close")
			closeButton.setSize(24f, 24f)
			closeButton.addClickListener {
				closeButton.remove()
				table.remove()
				openCount--
			}
			closeButton.setPosition(Global.stage.width - 35 - xPad, Global.stage.height - 50)

			Global.stage.addActor(closeButton)

			openCount++
		}

		fun createCard(content: Table, point: Vector2)
		{
			val cardWidget = CardWidget(content, content, AssetManager.loadTextureRegion("white")!!, null)
			cardWidget.setSize(24f, 24f)
			cardWidget.setPosition(point.x, point.y)

			Global.stage.addActor(cardWidget)

			cardWidget.focus()

			cardWidget.collapseFun = {
				cardWidget.remove()
			}
		}
	}
}