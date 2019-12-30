package com.lyeeedar.Util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.StringBuilder
import com.badlogic.gdx.utils.XmlReader
import kotlin.coroutines.experimental.buildSequence

fun getXml(path: String, extension: String = "xml"): XmlData
{
	try
	{
		var filepath = path
		if (!filepath.endsWith(".$extension"))
		{
			filepath += ".$extension"
		}

		return XmlData.getXml(filepath)
	}
	catch (ex: Exception)
	{
		System.err.println("Failed to load '$path'!")
		System.err.println(ex.message)
		throw ex
	}
}

fun getRawXml(path: String, extension: String = "xml"): XmlReader.Element
{
	try
	{
		var filepath = path
		if (!filepath.endsWith("." + extension))
		{
			filepath += "." + extension
		}

		var handle = Gdx.files.internal(filepath)
		if (!handle.exists()) handle = Gdx.files.absolute(filepath)
		return getRawXml(handle)
	}
	catch (ex: Exception)
	{
		System.err.println(ex.message)
		throw ex
	}
}

fun getRawXml(filehandle: FileHandle): XmlReader.Element
{
	try
	{
		return XmlReader().parse(filehandle)
	}
	catch (ex: Exception)
	{
		System.err.println(ex.message)
		throw ex
	}
}

fun XmlData.ranChild() = this.getChild(Random.random(this.childCount-1))

fun XmlReader.Element.ranChild() = this.getChild(Random.random(this.childCount-1))!!

fun XmlReader.Element.children(): Sequence<XmlReader.Element>
{
	val el = this
	return buildSequence {
		for (i in 0 until el.childCount)
		{
			yield(el.getChild(i))
		}
	}
}

operator fun XmlReader.Element.iterator(): Iterator<XmlReader.Element> = this.children().iterator()

fun XmlReader.Element.getChildrenByAttributeRecursively(attribute: String, value: String, result: Array<XmlReader.Element> = Array()): Array<XmlReader.Element>
{
	if (this.children().count() == 0) return result
	for (child in this.children())
	{
		if (child.getAttribute(attribute, null) == value) result.add(child)

		child.getChildrenByAttributeRecursively(attribute, value, result)
	}

	return result
}

fun XmlReader.Element.getChildrenRecursively(out: Array<XmlReader.Element> = Array()) : Array<XmlReader.Element>
{
	for (i in 0 until this.childCount)
	{
		val el = getChild(i)
		out.add(el)
		el.getChildrenRecursively(out)
	}

	return out
}

fun XmlData.toCharGrid(): Array2D<Char>
{
	val grid = Array2D<Char>(this.getChild(0).text.length, this.childCount) {x, y -> ' '}

	for (y in 0 until this.childCount)
	{
		val lineContents = this.getChild(y)
		for (x in 0 until lineContents.text.length)
		{
			grid[x, y] = lineContents.text[x]
		}
	}

	return grid
}

fun XmlData.toHitPointArray(): com.badlogic.gdx.utils.Array<Point>
{
	val grid = this.toCharGrid()
	val center = Point(-1, -1)

	outer@ for (x in 0 until grid.width)
	{
		for (y in 0 until grid.height)
		{
			if (grid[x, y] == '@')
			{
				center.set(x, y)
				break@outer
			}
		}
	}

	if (center.x == -1 || center.y == -1)
	{
		center.x = grid.width/2
		center.y = grid.height/2
	}

	val hitPoints = Array<Point>()

	for (x in 0 until grid.width)
	{
		for (y in 0 until grid.height)
		{
			if (grid[x, y] == '#')
			{
				val dx = x - center.x
				val dy = center.y - y

				hitPoints.add(Point(dx, dy))
			}
		}
	}

	return hitPoints
}

fun XmlReader.Element.toCompactString(): String
{
	val buffer = StringBuilder(128)
	buffer.append('<')
	buffer.append(name)
	if (this.attributes != null)
	{
		for (entry in this.attributes.entries())
		{
			buffer.append(' ')
			buffer.append(entry.key)
			buffer.append("=\"")
			buffer.append(entry.value)
			buffer.append('\"')
		}
	}
	if (this.childCount == 0 && (text == null || text.isEmpty()))
		buffer.append("/>")
	else
	{
		buffer.append(">")
		if (text != null && text.isNotEmpty())
		{
			buffer.append(text)
		}
		for (child in this.children())
		{
			buffer.append(child.toCompactString())
		}
		buffer.append("</")
		buffer.append(name)
		buffer.append('>')
	}
	return buffer.toString()
}