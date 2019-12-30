package com.lyeeedar.Util

import com.lyeeedar.Game.Global

fun String.expandVariables(): String
{
	val split = this.split('{')
	if (split.size == 1)
	{
		return this
	}
	else
	{
		val variables = Global.getVariableMap()

		var output = ""

		var isVar = this[0] == '{'
		for (word in split)
		{
			if (isVar)
			{
				val clean = word.replace("}", "").toLowerCase()
				val variable = variables[clean, 0.0f]

				if (variable.toInt().toFloat() == variable)
				{
					output += variable.toInt().toString()
				}
				else
				{
					output += variable.toString()
				}
			}
			else
			{
				output += word
			}

			isVar = !isVar
		}

		return output
	}
}