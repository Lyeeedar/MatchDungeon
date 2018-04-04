package com.lyeeedar.Renderables.Animation

import com.lyeeedar.Util.Colour

/**
 * Created by Philip on 31-Jul-16.
 */

abstract class AbstractScaleAnimation() : AbstractAnimation()
{
	override fun renderOffset(screenPositionMode: Boolean): FloatArray? = null
	override fun renderColour(): Colour? = null
	override fun renderRotation(): Float? = null
}