package com.lyeeedar.Util

fun packBytesToFloat(c1: Byte, c2: Byte, c3: Byte, c4: Byte): Float
{
	val temp = (c1.toInt() shl 24)
		.or(c2.toInt() shl 16)
		.or(c3.toInt() shl 8)
		.or(c4.toInt())
	return Float.fromBits(temp)
}

fun packBytesToInt(c1: Char, c2: Char, c3: Char, c4: Char): Int
{
	val temp = (c1.toInt())
		.or(c2.toInt() shl 8)
		.or(c3.toInt() shl 16)
		.or(c4.toInt() shl 24)
	return temp
}

fun packFloats(r: Float, g: Float, b: Float, a: Float): Float
{
	val intBits = (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
	return java.lang.Float.intBitsToFloat(intBits)
}