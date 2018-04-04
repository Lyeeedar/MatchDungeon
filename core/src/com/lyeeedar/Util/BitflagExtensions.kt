package com.lyeeedar.Util

// INT
inline fun Int.isFlagSet(index: Int): Boolean
{
	return this.and(1 shl index) != 0
}

inline fun Int.setFlag(index: Int): Int
{
	return this.or(1 shl index)
}

inline fun Int.unsetFlag(index: Int): Int
{
	return this.and((1 shl index).inv())
}

inline fun Int.noFlags(): Int = 0
inline fun Int.allFlags(): Int = intAllFlags
val intAllFlags = 0.inv()

// LONG
inline fun Long.isFlagSet(index: Int): Boolean
{
	return this.and(1L shl index) != 0.toLong()
}

inline fun Long.setFlag(index: Int): Long
{
	return this.or(1L shl index)
}

inline fun Long.unsetFlag(index: Int): Long
{
	return this.and((1L shl index).inv())
}

inline fun Long.noFlags(): Long = 0L
inline fun Long.allFlags(): Long = longAllFlags
val longAllFlags = 0L.inv()