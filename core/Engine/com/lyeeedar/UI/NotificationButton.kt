package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour

class NotificationButton(text: String, skin: Skin, val styleName: String = "default") : Table()
{
	val borderedCircle = AssetManager.loadTextureRegion("borderedcircle")!!
	val redColour = Colour(0.85f, 0f, 0f, 1f)

	val button = TextButton(text, skin, styleName)
	val notificationTable = Table()

	var text: String
		get() = button.text.toString()
		set(value)
		{
			button.setText(value)
		}

	init
	{
		this.skin = skin

		val stack = Stack()
		stack.add(button)
		stack.add(notificationTable)

		notificationTable.touchable = Touchable.disabled

		add(stack).grow()
	}

	fun clearNotification()
	{
		notificationTable.clear()
	}

	fun setNotification(img: TextureRegion)
	{
		clearNotification()

		val notificationStack = Stack()
		notificationStack.add(SpriteWidget(Sprite(borderedCircle, colour = redColour), 12f, 12f))
		notificationStack.add(SpriteWidget(Sprite(img), 12f, 12f))

		notificationTable.add(notificationStack).size(12f).expand().right().bottom().pad(2f)
	}
}