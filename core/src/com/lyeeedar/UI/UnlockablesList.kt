package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.lyeeedar.Global
import com.lyeeedar.Util.AssetManager

/**
 * Created by Philip on 06-Aug-16.
 */

class UnlockablesList<T: Unlockable>(val current: String?, val tree: UnlockTree<T>, val func: (String?) -> Unit, val displayCondition: (T) -> Boolean): FullscreenTable()
{
	val emptySlot = AssetManager.loadSprite("Icons/Empty")
	val table = Table()

	init
	{
		table.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24))

		this.add(table).expand().fill().pad(15f)

		val stack = Stack()
		table.add(stack).expand().fill()

		// build items
		val itemTable = Table()
		itemTable.defaults().pad(5f)

		if (current != null)
		{
			itemTable.add(createButton(getItem(current))).expandX().fillX()
			itemTable.row()
		}

		itemTable.add(createButton(null)).expandX().fillX()
		itemTable.row()

		for (item in tree.boughtDescendants())
		{
			if (displayCondition(item.value))
			{
				itemTable.add(createButton(item.value)).expandX().fillX()
				itemTable.row()
			}
		}

		val scroll = ScrollPane(itemTable)
		scroll.scrollTo(0f, 0f, 0f, 0f)
		stack.add(scroll)

		// close button
		val closeButton = Button(Global.skin, "close")
		closeButton.setSize(24f, 24f)
		closeButton.addClickListener({ remove() })
		val closeTable = Table()
		closeTable.add(closeButton).width(24f).height(24f).expand().top().right()
		stack.add(closeTable)
	}

	fun getItem(name: String): Unlockable?
	{
		for (item in tree.boughtDescendants())
		{
			if (item.key == name)
			{
				return item.value
			}
		}

		return null
	}

	fun createButton(item: Unlockable?): Button
	{
		val sprite = item?.icon?.copy() ?: emptySlot.copy()
		val name = item?.name ?: "Empty"
		val description = item?.description
		val stats = item?.stats()

		val textTable = Table()
		textTable.add(Label(name, Global.skin, "title")).expand().fill().left()

		if (description != null)
		{
			textTable.row()
			val label = Label(description, Global.skin)
			label.setWrap(true)
			textTable.add(label).expand().fill().left()
		}

		if (stats != null)
		{
			textTable.row()
			val label = Label(stats, Global.skin)
			label.setWrap(true)
			textTable.add(label).expand().fill().left()
		}

		val spriteWidget = SpriteWidget(sprite, 48f, 48f)

		val button = Button(Global.skin)
		button.add(spriteWidget).padRight(10f).padLeft(10f)
		button.add(textTable).expand().fill()

		button.addClickListener {
			func(item?.key)
			remove()
		}

		return button
	}
}