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
	if (count != 1)
	{
		if (this.endsWith('y'))
		{
			return this.substring(0, this.length - 1) + "ies"
		}

		return this + "s"
	}

	return this
}

fun String.addSpaces(): String
{
	val build = StringBuilder()
	for (char in this)
	{
		if (build.isEmpty())
		{
			build.append(char)
		}
		else if (char.isLetter() && char.isUpperCase())
		{
			build.append(' ')
			build.append(char)
		}
		else
		{
			build.append(char)
		}
	}

	return build.toString()
}