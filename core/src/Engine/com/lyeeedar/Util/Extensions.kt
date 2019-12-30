package com.lyeeedar.Util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Pool

/**
 * Created by Philip on 04-Jul-16.
 */

fun String.neaten() = this.substring(0, 1).toUpperCase() + this.substring(1).toLowerCase()

fun Char.isVowel(): Boolean
{
	return when(this.toLowerCase())
	{
		'a', 'e', 'i', 'o', 'u' -> true
		else -> false
	}
}

fun Long.millisToPrettyTime(showHours: Boolean = true, showMinutes: Boolean = true, showSeconds: Boolean = true): String
{
	val difference = this
	val seconds = difference / 1000L
	val minutes = seconds / 60L
	val hours = minutes / 60L

	val displayHours = kotlin.math.max(hours, 0)
	val displayMinutes = kotlin.math.max(minutes - hours * 60L, 0)
	val displaySeconds = kotlin.math.max(seconds - minutes * 60L, 0)

	var output = ""
	if (showHours) output += "$displayHours" + " hour".pluralize(displayHours.toInt())
	if (showMinutes) output += " $displayMinutes" + " minute".pluralize(displayMinutes.toInt())
	if (showSeconds) output += " $displaySeconds" + " second".pluralize(displaySeconds.toInt())

	return output
}


fun Float.toString(sigFig: Int): String
{
	return String.format("%.${sigFig}f", this)
}

fun Int.prettyPrint(): String
{
	if (this < 10000)
	{
		return this.toString()
	}
	else
	{
		return String.format("%,d", this)
	}
}

fun Color.toHSV(out: FloatArray? = null): FloatArray
{
	val max = Math.max(this.r, Math.max(this.g, this.b))
	val min = Math.min(this.r, Math.min(this.g, this.b))
	val delta = max - min

	val saturation = if (delta == 0f) 0f else delta / max
	val hue = if (this.r == max) ((this.g - this.b) / delta) % 6
				else if (this.g == max) 2 + (this.b - this.r) / delta
					else 4 + (this.r - this.g) / delta
	val value = max

	val output = if (out != null && out.size >= 3) out else kotlin.FloatArray(3)
	output[0] = (hue * 60f) / 360f
	output[1] = saturation
	output[2] = value

	return output
}


val vector2Pool = object : Pool<Vector2>() {
	override fun newObject(): Vector2
	{
		return Vector2()
	}
}

fun Vector2.freeTS()
{
	synchronized(vector2Pool)
	{
		vector2Pool.free(this)
	}
}

fun obtainVector2TS(): Vector2
{
	synchronized(vector2Pool)
	{
		return vector2Pool.obtain()
	}
}