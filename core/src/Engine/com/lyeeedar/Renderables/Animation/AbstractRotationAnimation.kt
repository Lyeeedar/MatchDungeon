package com.lyeeedar.Renderables.Animation

import com.lyeeedar.Util.Colour

abstract class AbstractRotationAnimation : AbstractAnimation()
{
	override fun renderScale(): FloatArray? = null
	override fun renderOffset(screenPositionMode: Boolean): FloatArray? = null
	override fun renderColour(): Colour? = null
}