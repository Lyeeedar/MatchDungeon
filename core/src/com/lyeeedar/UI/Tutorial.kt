package com.lyeeedar.UI

import com.badlogic.gdx.utils.Array
import com.lyeeedar.Global

class Tutorial(val key: String)
{
	val actions = Array<Any>()
	var index: Int = -1
	var currentAction: ((Float) -> Boolean)? = null

	fun addPopup(text: String, emphasis: Any)
	{
		actions.add(TutorialPopup(text, emphasis, {
			advance()
		}))
	}
	fun addDelay(duration: Float)
	{
		var counter = 0f
		val action = fun (delta:Float): Boolean {
			counter += delta
			if (counter >= duration)
			{
				return true
			}
			return false
		}
		actions.add(action)
	}
	fun addAction(action: (Float) -> Boolean)
	{
		actions.add(action)
	}

	fun act(delta: Float)
	{
		val complete = currentAction?.invoke(delta) ?: false
		if (complete)
		{
			currentAction = null
			advance()
		}
	}

	fun show()
	{
		if (Global.settings.get(key, false))
		{
			return
		}

		if (current != null)
		{
			if (!queue.any{ it.key == key }) queue.add(this)
			return
		}

		Global.settings.set(key, true)

		current = this

		advance()
	}

	private fun advance()
	{
		index++
		if (index >= actions.size)
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
			val action = actions[index]
			if (action is TutorialPopup)
			{
				action.show()
			}
			else
			{
				currentAction = action as ((Float) -> Boolean)?
			}
		}
	}

	companion object
	{
		var current: Tutorial? = null
		val queue = Array<Tutorial>()
	}
}