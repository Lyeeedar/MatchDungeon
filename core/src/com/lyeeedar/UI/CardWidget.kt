package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Direction
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.min
import ktx.actors.alpha
import ktx.actors.then

data class Pick(val string: String, var pickFun: (card: CardWidget) -> Unit)

class CardWidget(val frontTable: Table, val frontDetailTable: Table, val backImage: TextureRegion, val data: Any?) : Widget()
{
	val referenceWidth = Global.resolution.x - 100f
	val referenceHeight = Global.resolution.y - 200f

	val contentTable = Table()
	val backTable: Table

	var canZoom = true
	var canPickFaceDown = false

	val pickFuns = Array<Pick>()

	private var faceup = false
	private var flipping = false
	private var fullscreen = false

	var clickable = true

	val back = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 30, 30, 30, 30)

	init
	{
		//debug()

		contentTable.background = NinePatchDrawable(back)

		backTable = Table()
		backTable.add(SpriteWidget(Sprite(backImage), referenceWidth - 60, referenceWidth - 60)).expand().center()

		contentTable.add(backTable).grow()

		contentTable.isTransform = true
		contentTable.originX = referenceWidth / 2
		contentTable.originY = referenceHeight / 2

		contentTable.setSize(referenceWidth, referenceHeight)

		addClickListener {
			if (clickable)
			{
				if (canZoom)
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
				else
				{
					if (canPickFaceDown || faceup)
					{
						if (pickFuns.size > 0)
						{
							pickFuns[0].pickFun(this)
						}
					}
					else
					{
						flip(true)
					}
				}
			}
		}

		//debug()
	}

	fun addPick(string: String, pickFun: (card: CardWidget) -> Unit)
	{
		pickFuns.add(Pick(string, pickFun))
	}

	data class Bounds(val x: Float, val y: Float, val width: Float, val height: Float)
	fun getTableBounds(): Bounds
	{
		val actualMidX = contentTable.x + contentTable.width / 2f
		val drawX = actualMidX - (contentTable.width * contentTable.scaleX) / 2f

		val actualMidY = contentTable.y + contentTable.height / 2f
		val drawY = actualMidY - (contentTable.height * contentTable.scaleY) / 2f

		return Bounds(drawX, drawY, contentTable.width * contentTable.scaleX, contentTable.height * contentTable.scaleY)
	}

	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		val (tablex, tabley, width, height) = getTableBounds()
		val minx = tablex - this.x
		val miny = tabley - this.y

		if (touchable && this.touchable != Touchable.enabled) return null
		return if (x >= minx && x < minx+width && y >= miny && y < miny+height) this else null
	}

	override fun act(delta: Float)
	{
		super.act(delta)

		if (!fullscreen)
		{
			contentTable.act(delta)
		}
	}

	override fun setBounds(x: Float, y: Float, width: Float, height: Float)
	{
		super.setBounds(x, y, width, height)

		val scaleX = width / referenceWidth
		val scaleY = height / referenceHeight
		val min = min(scaleX, scaleY)

		contentTable.setScale(min)

		val middleX = x + width / 2f
		val middleY = y + height / 2f

		contentTable.setPosition(middleX - contentTable.width / 2f, middleY - contentTable.height / 2f)
	}

	override fun positionChanged()
	{
		super.positionChanged()
		setBounds(x, y, width, height)
	}

	override fun rotationChanged()
	{
		super.rotationChanged()
		contentTable.rotation = rotation
	}

	override fun sizeChanged()
	{
		super.sizeChanged()
		setBounds(x, y, width, height)
	}

	override fun drawDebug(shapes: ShapeRenderer)
	{
		super.drawDebug(shapes)

		val bounds = getTableBounds()

		shapes.color = Color.OLIVE
		shapes.set(ShapeRenderer.ShapeType.Line)
		shapes.rect(x, y, width, height)

		shapes.set(ShapeRenderer.ShapeType.Line)
		shapes.color = Color.MAGENTA
		shapes.rect(bounds.x, bounds.y, bounds.width, bounds.height)
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

			val stack = Stack()
			stack.add(frontDetailTable)
			stack.add(frontTable)
			contentTable.add(stack).grow()

			val hideSequence = alpha(1f) then fadeOut(speed)
			val showSequence = alpha(0f) then fadeIn(speed)

			frontDetailTable.addAction(hideSequence)
			frontTable.addAction(showSequence)
		} then parallel(scaleTo(currentScale, currentScale, speed), moveTo(trueCurrentX, trueCurrentY, speed)) then lambda {
			fullscreen = false

			background.remove()
			table.remove()
			table.clear()

			contentTable.clear()
			contentTable.add(frontTable).grow()

			contentTable.setScale(currentScale)
			contentTable.setPosition(currentX, currentY)
		}

		val expandSequence = lambda {
			contentTable.clear()

			val stack = Stack()
			stack.add(frontDetailTable)
			stack.add(frontTable)
			contentTable.add(stack).grow()

			val hideSequence = alpha(1f) then fadeOut(speed)
			val showSequence = alpha(0f) then fadeIn(speed)

			frontTable.addAction(hideSequence)
			frontDetailTable.addAction(showSequence)
		} then parallel(scaleTo(1f, 1f, speed), moveTo(destX, destY, speed)) then lambda {
			contentTable.clear()

			val stack = Stack()
			stack.add(frontDetailTable)

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

			val pickButtonTable = Table()

			for (pick in pickFuns)
			{
				val pickButton =  TextButton(pick.string, Global.skin)
				pickButton.addClickListener {
					pick.pickFun(this)
					table.addAction(collapseSequence)
					background.addAction(fadeOut(speed))
				}

				pickButtonTable.add(pickButton).uniform()
			}

			buttonTable.add(pickButtonTable).expand().bottom().pad(10f)
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