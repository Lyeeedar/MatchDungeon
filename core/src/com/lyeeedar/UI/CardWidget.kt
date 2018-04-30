package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.actions.Actions.scaleTo
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import ktx.actors.then

class CardWidget(val title: String, val description: String, val icon: Sprite, val back: NinePatch, val front: NinePatch, val pickString: String, val pickFun: () -> Unit) : Table()
{
	val frontTable: Table
	val backTable: Table

	private var faceup = false
	private var fullscreen = false

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

		frontTable.addClickListener {
			focus()
		}

		add(backTable).grow()
	}

	fun flip(animate: Boolean = true)
	{
		val nextTable = if (faceup) backTable else frontTable
		val flipFun = fun () { clear(); add(nextTable).grow() }

		if (animate)
		{
			val speed = 0.5f
			val sequence = scaleTo(0f, 1f, speed) then lambda { flipFun() } then scaleTo(1f, 1f, speed)
			addAction(sequence)
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

		val parent = parent

		val table = Table()
		remove()

		table.add(this)
		parent.addActor(table)
		table.setFillParent(true)

		clear()

		val stack = Stack()
		stack.add(frontTable)

		val buttonTable = Table()
		stack.add(table)

		add(stack).grow()

		val closeButton = Button(skin, "close")
		closeButton.addClickListener {

		}
		buttonTable.add(closeButton).right().top().pad(10f)
		buttonTable.row()

		val pickButton =  TextButton(pickString, skin)
		pickButton.addClickListener {
			pickFun()
		}
		buttonTable.add(pickButton).bottom().pad(10f)

		fullscreen = true
	}
}