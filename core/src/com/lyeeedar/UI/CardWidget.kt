package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.min
import ktx.actors.alpha
import ktx.actors.parallelTo
import ktx.actors.then

class CardWidget(val frontTable: Table, val pickString: String, val data: Any?, val pickFun: (card: CardWidget) -> Unit) : Widget()
{
	val referenceWidth = Global.resolution.x - 100f
	val referenceHeight = Global.resolution.y - 200f

	val contentTable = Table()
	val backTable: Table

	private var faceup = false
	private var flipping = false
	private var fullscreen = false

	var clickable = true

	val back = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 16, 16, 16, 16)
	val front = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 16, 16, 16, 16)

	init
	{
		contentTable.background = NinePatchDrawable(front)

		backTable = Table()
		backTable.background = NinePatchDrawable(back)

		contentTable.add(backTable).grow()

		contentTable.isTransform = true
		contentTable.originX = referenceWidth / 2
		//contentTable.originY = referenceHeight / 2

		contentTable.setSize(referenceWidth, referenceHeight)

		addClickListener {
			if (clickable)
			{
				if (!fullscreen && faceup)
				{
					focus()
				}
				else if (!faceup)
				{
					flip(true)
				}
			}
		}
	}

	data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
	fun getTableBounds(): Bounds
	{
		val tableWidth = ((referenceWidth-contentTable.originX) * contentTable.scaleX) + contentTable.originX
		val tableHeight = ((referenceHeight-contentTable.originY) * contentTable.scaleY) + contentTable.originY

		val tableX = (-contentTable.originX * contentTable.scaleX) + contentTable.originX
		val tableY = (-contentTable.originY * contentTable.scaleY) + contentTable.originY

		return Bounds(x + tableX, y + tableY, tableWidth, tableHeight)
	}

	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		val (tablex, tabley, width, height) = getTableBounds()
		val minx = tablex - this.x
		val miny = tabley - this.y

		if (touchable && this.touchable != Touchable.enabled) return null
		return if (x >= minx && x < width && y >= miny && y < height) this else null
	}

	override fun act(delta: Float)
	{
		super.act(delta)
		contentTable.act(delta)
	}

	override fun setBounds(x: Float, y: Float, width: Float, height: Float)
	{
		super.setBounds(x, y, width, height)

		val scaleX = width / referenceWidth
		val scaleY = height / referenceHeight
		val min = min(scaleX, scaleY)

		contentTable.setScale(min)

		val tableX = (width / 2f) - contentTable.originX
		contentTable.setPosition(x + tableX, y)
	}

	override fun positionChanged()
	{
		super.positionChanged()
		setBounds(x, y, width, height)
	}

	override fun sizeChanged()
	{
		super.sizeChanged()
		setBounds(x, y, width, height)
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		super.draw(batch, parentAlpha)

		if (!fullscreen)
		{
			contentTable.draw(batch, parentAlpha * alpha)
		}
	}

	fun setFacing(faceup: Boolean, animate: Boolean = true)
	{
		if (faceup != this.faceup)
		{
			flip(animate)
		}
	}

	fun flip(animate: Boolean = true)
	{
		if (flipping) return
		flipping = true

		val nextTable = if (faceup) backTable else frontTable
		val flipFun = fun () { contentTable.clearChildren(); contentTable.add(nextTable).grow(); flipping = false }

		if (animate)
		{
			val scale = contentTable.scaleX
			val speed = 0.1f
			val sequence = scaleTo(0f, scale, speed) then lambda { flipFun() } then scaleTo(scale, scale, speed)
			contentTable.addAction(sequence)
		}
		else
		{
			flipFun()
		}

		faceup = !faceup
	}

	fun focus()
	{
		if (fullscreen) { return }
		fullscreen = true

		val table = Table()
		val background = Table()

		val (trueCurrentX, trueCurrentY, currentwidth, currentheight) = getTableBounds()
		val currentX = contentTable.x
		val currentY = contentTable.y
		val currentScale = contentTable.scaleX

		// Setup anim
		val speed = 0.1f
		val destX = (stage.width - referenceWidth) / 2f
		val destY = (stage.height - referenceHeight) / 2f

		val collapseSequence = lambda {
			contentTable.clear()
			contentTable.add(if (faceup) frontTable else backTable).grow()
		} then parallel(scaleTo(currentScale, currentScale, speed), moveTo(trueCurrentX, trueCurrentY, speed)) then lambda {
			fullscreen = false

			background.remove()
			table.remove()
			table.clear()

			contentTable.setScale(currentScale)
			contentTable.setPosition(currentX, currentY)
		}

		val expandSequence = scaleTo(1f, 1f, speed) parallelTo moveTo(destX, destY, speed) then lambda {
			contentTable.clear()

			val stack = Stack()
			stack.add(frontTable)

			val buttonTable = Table()
			stack.add(buttonTable)

			contentTable.add(stack).grow()

			val closeButton = Button(Global.skin, "close")
			closeButton.addClickListener {
				table.addAction(collapseSequence)
				background.addAction(fadeOut(speed))
			}
			buttonTable.add(closeButton).expand().right().top().size(24f).pad(10f)
			buttonTable.row()

			val pickButton =  TextButton(pickString, Global.skin)
			pickButton.addClickListener {
				pickFun(this)
				table.addAction(collapseSequence)
				background.addAction(fadeOut(speed))
			}
			buttonTable.add(pickButton).expand().bottom().pad(10f)
		}

		// Create holder
		table.touchable = Touchable.enabled
		table.isTransform = true

		table.add(contentTable).grow()
		contentTable.setScale(1f)

		table.setSize(referenceWidth, referenceHeight)
		table.setPosition(trueCurrentX, trueCurrentY)
		table.setScale(currentScale)
		table.addAction(expandSequence)
		table.addClickListener {

		}

		// Background
		background.touchable = Touchable.enabled
		background.setFillParent(true)
		background.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, 0.5f))
		background.addClickListener {
			table.addAction(collapseSequence)
			background.addAction(fadeOut(speed))
		}
		background.alpha = 0f
		background.addAction(fadeIn(speed))

		Global.stage.addActor(background)
		Global.stage.addActor(table)
	}

	companion object
	{
		fun layoutCards(cardWidgets: Array<CardWidget>, enterFrom: Direction)
		{
			// Calculate card sizes
			val padding = 20f
			val cardWidth = (Global.resolution.x - 3f * padding) / 2f
			val cardHeight = (Global.resolution.y - 3f * padding) / 2f

			// Calculate final positions
			if (cardWidgets.size == 1)
			{
				// center
				cardWidgets[0].setPosition(Global.resolution.x / 2f - cardWidth / 2f, Global.resolution.y / 2f - cardHeight / 2f)
			}
			else if (cardWidgets.size == 2)
			{
				// vertical alignment
				cardWidgets[0].setPosition(Global.resolution.x / 2f - cardWidth / 2f, padding * 2f + cardHeight)
				cardWidgets[1].setPosition(Global.resolution.x / 2f - cardWidth / 2f, padding)
			}
			else if (cardWidgets.size == 3)
			{
				// triangle, single card at top, 2 below
				cardWidgets[0].setPosition(Global.resolution.x / 2f - cardWidth / 2f, padding * 2f + cardHeight)
				cardWidgets[1].setPosition(padding, padding)
				cardWidgets[2].setPosition(padding * 2f + cardWidth, padding)
			}
			else if (cardWidgets.size == 4)
			{
				// even grid
				cardWidgets[0].setPosition(padding, padding * 2f + cardHeight)
				cardWidgets[1].setPosition(padding, padding)
				cardWidgets[2].setPosition(padding * 2f + cardWidth, padding)
				cardWidgets[3].setPosition(padding * 2f + cardWidth, padding * 2f + cardHeight)
			}

			// calculate start position
			val startX: Float = when (enterFrom)
			{
				Direction.CENTER -> Global.resolution.x / 2f - cardWidth / 2f
				Direction.NORTH -> Global.resolution.x / 2f - cardWidth / 2f
				Direction.SOUTH -> Global.resolution.x / 2f - cardWidth / 2f

				Direction.EAST -> Global.resolution.x.toFloat()
				Direction.NORTHEAST -> Global.resolution.x.toFloat()
				Direction.SOUTHEAST -> Global.resolution.x.toFloat()

				Direction.WEST -> -cardWidth
				Direction.NORTHWEST -> -cardWidth
				Direction.SOUTHWEST -> -cardWidth
			}

			val startY: Float = when (enterFrom)
			{
				Direction.CENTER -> Global.resolution.y / 2f - cardHeight / 2f
				Direction.EAST -> Global.resolution.y / 2f - cardHeight / 2f
				Direction.WEST -> Global.resolution.y / 2f - cardHeight / 2f

				Direction.NORTH -> Global.resolution.y.toFloat()
				Direction.NORTHWEST -> Global.resolution.y.toFloat()
				Direction.NORTHEAST -> Global.resolution.y.toFloat()

				Direction.SOUTH -> -cardHeight
				Direction.SOUTHWEST -> -cardHeight
				Direction.SOUTHEAST -> -cardHeight
			}

			// do animation
			var delay = 0.2f
			for (widget in cardWidgets)
			{
				val x = widget.x
				val y = widget.y

				widget.setSize(cardWidth, cardHeight)
				widget.setPosition(startX, startY)
				widget.clickable = false

				val delayVal = delay
				delay += 0.04f
				val sequence = delay(delayVal) then moveTo(x, y, 0.2f) then delay(0.1f) then lambda { widget.flip(true); widget.clickable = true }
				widget.addAction(sequence)
			}
		}
	}
}