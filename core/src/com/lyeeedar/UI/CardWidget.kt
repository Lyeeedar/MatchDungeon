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
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.min
import ktx.actors.alpha
import ktx.actors.parallelTo
import ktx.actors.then

class CardWidget(val title: String, val description: String, val icon: Sprite, val pickString: String, val pickFun: () -> Unit) : Widget()
{
	val referenceWidth = 180f
	val referenceHeight = 320f

	val contentTable = Table()

	val frontTable: Table
	val backTable: Table

	private var faceup = false
	private var flipping = false
	private var fullscreen = false

	val back = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 16, 16, 16, 16)
	val front = NinePatch(AssetManager.loadTextureRegion("GUI/CardBackground"), 16, 16, 16, 16)

	init
	{
		val skin = Global.skin

		// create foreground
		frontTable = Table()
		frontTable.background = NinePatchDrawable(front)

		val title = Label(title, skin, "title")
		title.setWrap(true)

		val desc = Label(description, skin)
		desc.setWrap(true)

		frontTable.add(title)
		frontTable.row()
		frontTable.add(SpriteWidget(icon, 32f, 32f))
		frontTable.row()
		frontTable.add(desc)

		// create background
		backTable = Table()
		backTable.background = NinePatchDrawable(back)

		contentTable.add(backTable).grow()

		contentTable.isTransform = true

		contentTable.setSize(referenceWidth, referenceHeight)

		addClickListener {
			if (!fullscreen && faceup)
			{
				focus()
			}
		}
	}

	override fun hit(x: Float, y: Float, touchable: Boolean): Actor?
	{
		if (touchable && this.touchable != Touchable.enabled) return null
		return if (x >= 0 && x < contentTable.width * contentTable.scaleX && y >= 0 && y < contentTable.height * contentTable.scaleY) this else null
	}

	override fun setBounds(x: Float, y: Float, width: Float, height: Float)
	{
		super.setBounds(x, y, width, height)

		val scaleX = width / referenceWidth
		val scaleY = height / referenceHeight
		val min = min(scaleX, scaleY)

		contentTable.setScale(min)
		contentTable.setPosition(x, y)
	}

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		super.draw(batch, parentAlpha)

		if (!fullscreen)
		{
			contentTable.draw(batch, parentAlpha)
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
		val flipFun = fun () { contentTable.clear(); contentTable.add(nextTable).grow(); flipping = false }

		if (animate)
		{
			val speed = 0.5f
			val sequence = scaleTo(0f, 1f, speed) then lambda { flipFun() } then scaleTo(1f, 1f, speed)
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

		val currentX = x
		val currentY = y
		val currentScale = contentTable.scaleX

		// Setup anim
		val speed = 0.1f
		val destX = (stage.width - referenceWidth) / 2f
		val destY = (stage.height - referenceHeight) / 2f

		val collapseSequence = lambda {
			contentTable.clear()
			contentTable.add(if (faceup) frontTable else backTable).grow()
		} then parallel(scaleTo(currentScale, currentScale, speed), moveTo(currentX, currentY, speed)) then lambda {
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
				pickFun()
				table.addAction(collapseSequence)
				background.addAction(fadeOut(speed))
			}
			buttonTable.add(pickButton).expand().bottom().pad(10f)
		}

		// Create holder
		table.isTransform = true

		table.add(contentTable).grow()
		contentTable.setScale(1f)

		table.setSize(referenceWidth, referenceHeight)
		table.setPosition(currentX, currentY)
		table.setScale(currentScale)
		table.addAction(expandSequence)

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
}