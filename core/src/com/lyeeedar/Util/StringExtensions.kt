package com.lyeeedar.Util

fun String.directory(): String
{
	val split = this.split('/')
	return split.subList(0, split.size-1).joinToString("/")
}

fun String.filename(extension: Boolean): String
{
	val split = this.split('/')
	if (extension)
	{
		return split.last()
	}
	else
	{
		val split2 = split.last().split('.')
		return split2.first()
	}
}

fun String.pluralize(count: Int): String
{
	if (count > 1)
	{
		return this + "s"
	}

	return this
}