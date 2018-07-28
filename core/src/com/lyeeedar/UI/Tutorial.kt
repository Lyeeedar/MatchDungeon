package com.lyeeedar.UI

import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.Array
import com.lyeeedar.Global

class Tutorial(val key: String)
{
	val popups = Array<TutorialPopup>()
	var index: Int = -1

	fun addPopup(text: String, emphasis: Actor) { addPopup(text, emphasis.getBounds()) }
	fun addPopup(text: String, emphasis: Rectangle)
	{
		popups.add(TutorialPopup(text, emphasis, {
			advance()
		}))
	}

	fun show()
	{
		if (current != null)
		{
			queue.add(this)
			return
		}

		if (true)//(!Global.settings.get(key, false))
		{
			Global.settings.set(key, true)

			current = this

			advance()
		}
	}

	private fun advance()
	{
		index++
		if (index >= popups.size)
		{
			current = null
			if (queue.size > 0)
			{
				val first = queue.removeIndex(0)
				first.show()
			}
		}
		else
		{
			val popup = popups[index]
			popup.show()
		}
	}

	companion object
	{
		var current: Tutorial? = null
		val queue = Array<Tutorial>()
	}
}