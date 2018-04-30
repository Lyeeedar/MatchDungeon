package com.lyeeedar.Util

fun String.directory(): String
{
	val split = this.split('/')
	return split.subList(0, split.size-1).joinToString("/")
}