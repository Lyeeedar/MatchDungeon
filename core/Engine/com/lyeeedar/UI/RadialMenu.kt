package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils.clamp
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics

class RadialMenu(val closeAction: () -> Unit) : Widget()
{
	val circle = AssetManager.loadTextureRegion("Sprites/GUI/RadialMenuBorder")!!
	val borderedcircle = AssetManager.loadTextureRegion("Sprites/borderedcircle")!!
	val white = AssetManager.loadTextureRegion("Sprites/white")!!
	val tick = AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_680")!!

	var tooltip: Tooltip? = null

	enum class Position
	{
		Bottom,
		Top
	}

	class MenuItem(val icon: TextureRegion, val tooltip: String, val previewAction: () -> Unit, val depreviewAction: () -> Unit, val clickAction: () -> Unit, val dockPosition: Position)
	{
		var previewed = false
		var assignedAngle: Float = 0f
		val glyphCache = GlyphLayout()
	}

	val menuSize = 80f
	val itemSize = 32f

	var clickPos: Vector2 = Vector2()
	val items = Array<MenuItem>()

	fun addItem(icon: TextureRegion, tooltip: String,  previewAction: () -> Unit, depreviewAction: () -> Unit, clickAction: () -> Unit, dockPosition: Position)
	{
		val item = MenuItem(icon, tooltip, previewAction, depreviewAction, clickAction, dockPosition)
		items.add(item)

		val font = Statics.skin.getFont("default")
		item.glyphCache.setText(font, item.tooltip, Color.WHITE, itemSize * 2f, Align.center, true)

		assignAngles()
	}

	var backgroundTable = Table()
	fun show()
	{
		backgroundTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.0f))
		backgroundTable.touchable = Touchable.enabled
		backgroundTable.setFillParent(true)

		backgroundTable.addClickListenerFull {
				inputEvent, x, y ->

			val menux = clamp(clickPos.x - menuSize / 2f, itemSize/2f, Statics.stage.width - menuSize - itemSize/2f)
			val menuy = clamp(clickPos.y - menuSize / 2f, itemSize/2f, Statics.stage.height - menuSize - itemSize/2f)

			val centerx = (menux + menuSize / 2f)
			val centery = (menuy + menuSize / 2f)

			val itemx = centerx - itemSize / 2f
			val itemy = centery - itemSize / 2f

			var clicked = false
			for (item in items)
			{
				val vec = Vector2(0f, menuSize/2f)
				vec.rotate(item.assignedAngle)

				val minx = itemx + vec.x
				val maxx = minx + itemSize
				val miny = itemy + vec.y
				val maxy = miny + itemSize

				if (x in minx..maxx && y in miny..maxy)
				{
					if (item.previewed)
					{
						item.clickAction.invoke()
						close()
					}
					else
					{
						for (i in 0 until items.size)
						{
							val item = items[i]
							if (item.previewed)
							{
								item.depreviewAction.invoke()
								item.previewed = false

								tooltip?.remove()
							}
						}

						item.previewed = true
						item.previewAction.invoke()

						tooltip = Tooltip(item.tooltip, Statics.skin, Statics.stage)
						tooltip!!.width = (Statics.stage.width / 3f) * 2f
						tooltip!!.layout()

						val x = centerx - tooltip!!.width / 2f
						if (centery > Statics.stage.height / 2f)
						{
							tooltip!!.show(x, centery - menuSize / 2f - itemSize - tooltip!!.height)
						}
						else
						{
							tooltip!!.show(x, centery + menuSize / 2f + itemSize)
						}
					}

					clicked = true
					inputEvent?.handle()
					break
				}
			}

			if (!clicked)
			{
				close()
			}
		}

		Statics.stage.addActor(backgroundTable)
		Statics.stage.addActor(this)
	}

	fun close()
	{
		for (item in items)
		{
			if (item.previewed)
			{
				item.depreviewAction.invoke()
			}
		}

		backgroundTable.remove()
		remove()

		tooltip?.remove()

		closeAction.invoke()
	}

	fun assignAngles()
	{
		val angleStep = 360f / items.size

		val topItems = items.filter { it.dockPosition == Position.Top }
		val bottomItems = items.filter { it.dockPosition == Position.Bottom }

		var anglecurrent = -angleStep * (topItems.size.toFloat() / 2f)
		for (item in topItems)
		{
			item.assignedAngle = anglecurrent + angleStep / 2f
			anglecurrent += angleStep
		}

		anglecurrent = 180f + angleStep * (bottomItems.size.toFloat() / 2f)
		for (item in bottomItems)
		{
			item.assignedAngle = anglecurrent - angleStep / 2f
			anglecurrent -= angleStep
		}
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		val menux = clamp(clickPos.x - menuSize / 2f, itemSize/2f, Statics.stage.width - menuSize - itemSize/2f)
		val menuy = clamp(clickPos.y - menuSize / 2f, itemSize/2f, Statics.stage.height - menuSize - itemSize/2f)

		val centerx = (menux + menuSize / 2f)
		val centery = (menuy + menuSize / 2f)

		batch.color = Color.WHITE
		batch.draw(circle, menux, menuy, menuSize, menuSize)

		val itemx = centerx - itemSize / 2f
		val itemy = centery - itemSize / 2f

		for (item in items)
		{
			val vec = Vector2(0f, menuSize/2f)
			vec.rotate(item.assignedAngle)

			batch.color = Color.DARK_GRAY
			batch.draw(borderedcircle, itemx + vec.x, itemy + vec.y, itemSize, itemSize)

			batch.color = Color.WHITE
			val icon = if (item.previewed) tick else item.icon
			batch.draw(icon, itemx + vec.x, itemy + vec.y, itemSize, itemSize)
		}
	}
}