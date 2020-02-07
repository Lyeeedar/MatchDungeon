package com.lyeeedar.build.SourceRewriter

class IndentedStringBuilder
{
	val builder = StringBuilder()

	val multiSpaceRegex = "\\s+".toRegex()

	fun appendlnFix(indentation: Int, line: String)
	{
		appendln(indentation, line.replace(multiSpaceRegex," "))
	}

	fun appendln(indentation: Int, line: String)
	{
		val indentation = "\t".repeat(indentation)
		builder.appendln("$indentation$line")
	}

	fun appendln(line: String)
	{
		builder.appendln(line)
	}

	override fun toString(): String
	{
		return builder.toString()
	}
}