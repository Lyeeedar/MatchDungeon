package com.lyeeedar.build.SourceRewriter

class IndentedStringBuilder
{
	val builder = StringBuilder()

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