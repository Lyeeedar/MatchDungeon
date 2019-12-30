package com.exp4j.Helpers

import com.badlogic.gdx.utils.ObjectFloatMap

class EquationHelper
{
	companion object
	{
		fun evaluate(eqn: String, variableMap: ObjectFloatMap<String> = ObjectFloatMap(), seed: Long? = null): Float
		{
			val exp = CompiledExpression(eqn, variableMap)
			val value = exp.evaluate(variableMap, seed)
			exp.free()
			return value
		}
	}
}

fun String.unescapeCharacters(): String
{
	var output = this
	output = output.replace("&gt;", ">")
	output = output.replace("&lt;", "<")
	output = output.replace("&amp;", "&")
	return output
}

fun String.evaluate(variableMap: ObjectFloatMap<String> = ObjectFloatMap(), seed: Long? = null): Float = EquationHelper.evaluate(this, variableMap, seed)