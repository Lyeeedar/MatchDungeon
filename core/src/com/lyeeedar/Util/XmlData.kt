package com.lyeeedar.Util

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.XmlReader
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import ktx.collections.set
import kotlin.coroutines.experimental.buildSequence

class XmlData
{
	lateinit var name: String
	var nameId: Int = -1

	var children: Array<XmlData> = Array(0){_ -> XmlData()}
	val childMap: IntMap<XmlData> = IntMap()
	var lastIndex = 0

	val childCount: Int
		get() = children.size

	var attributeMap: IntMap<XmlAttributeData> = IntMap()

	var value: Any? = null

	constructor()
	{
	}

	constructor(handle: FileHandle)
	{
		load(handle)
	}

	constructor(name: String, data: String)
	{
		this.name = name
		this.nameId = name.toUpperCase().hashCode()
		this.value = data
	}

	fun getChild(index: Int) = children[index]

	fun children(): Array<XmlData> = children

	fun getChildByName(name: String): XmlData?
	{
		return getChildById(name.toUpperCase().hashCode())
	}

	fun getChildrenByName(name: String): Sequence<XmlData>
	{
		val id = name.toUpperCase().hashCode()
		return buildSequence {
			for (child in children)
			{
				if (child.nameId == id)
				{
					yield(child)
				}
			}
		}
	}

	fun getChildById(id: Int): XmlData?
	{
		if (children.isEmpty()) return null

		lastIndex++
		if (lastIndex == children.size) lastIndex = 0

		if (children[lastIndex].nameId == id)
		{
			return children[lastIndex]
		}

		return childMap[id]
	}

	fun get(name: String): String
	{
		return getChildByName(name)?.text ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun get(name: String, fallback: String?): String?
	{
		return getChildByName(name)?.text ?: fallback
	}

	fun getInt(name: String): Int
	{
		return getChildByName(name)?.int() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getInt(name: String, fallback: Int): Int
	{
		return getChildByName(name)?.int() ?: fallback
	}

	fun getFloat(name: String): Float
	{
		return getChildByName(name)?.float() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getFloat(name: String, fallback: Float): Float
	{
		return getChildByName(name)?.float() ?: fallback
	}

	fun getBoolean(name: String): Boolean
	{
		return getChildByName(name)?.boolean() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getBoolean(name: String, fallback: Boolean): Boolean
	{
		return getChildByName(name)?.boolean() ?: fallback
	}

	fun getLong(name: String): Long
	{
		return getChildByName(name)?.long() ?: throw GdxRuntimeException("Element ${this.name} has no child called $name!")
	}

	fun getLong(name: String, fallback: Long): Long
	{
		return getChildByName(name)?.long() ?: fallback
	}

	fun getPoint(name: String): Point
	{
		val str = get(name)
		val split = str.split(",")
		val x = split[0].toInt()
		val y = split[1].toInt()

		return Point(x, y)
	}

	fun getPoint(name: String, fallback: Point): Point
	{
		val str = get(name, null) ?: return fallback
		val split = str.split(",")
		val x = split[0].toInt()
		val y = split[1].toInt()

		return Point(x, y)
	}

	fun getAttribute(name: String): String
	{
		return attributeMap[name.toUpperCase().hashCode()]?.text() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getAttribute(name: String, fallback: String?): String?
	{
		return attributeMap[name.toUpperCase().hashCode()]?.text() ?: fallback
	}

	fun getIntAttribute(name: String): Int
	{
		return attributeMap[name.toUpperCase().hashCode()]?.int() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getIntAttribute(name: String, fallback: Int): Int
	{
		return attributeMap[name.toUpperCase().hashCode()]?.int() ?: fallback
	}

	fun getFloatAttribute(name: String): Float
	{
		return attributeMap[name.toUpperCase().hashCode()]?.float() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getFloatAttribute(name: String, fallback: Float): Float
	{
		return attributeMap[name.toUpperCase().hashCode()]?.float() ?: fallback
	}

	fun getBooleanAttribute(name: String): Boolean
	{
		return attributeMap[name.toUpperCase().hashCode()]?.boolean() ?: throw GdxRuntimeException("Element ${this.name} has no attribute called $name!")
	}

	fun getBooleanAttribute(name: String, fallback: Boolean): Boolean
	{
		return attributeMap[name.toUpperCase().hashCode()]?.boolean() ?: fallback
	}

	fun set(name: String, value: Any)
	{
		val holder = XmlData()
		holder.name = name
		holder.nameId = name.hashCode()
		holder.value = value

		addChild(holder)
	}

	fun addChild(xmlData: XmlData)
	{
		children = Array(childCount+1) { i -> if (i < childCount) children[i] else xmlData }
	}

	fun addChild(name: String): XmlData
	{
		val holder = XmlData()
		holder.name = name
		holder.nameId = name.hashCode()

		addChild(holder)

		return holder
	}

	val text: String
			get() = value?.toString() ?: ""
	fun int(): Int = value as? Int ?: value.toString().toIntOrNull() ?: throw TypeCastException("Cannot cast $value to an Int!")
	fun float(): Float = value as? Float ?: value.toString().toFloatOrNull() ?: throw TypeCastException("Cannot cast $value to a Float!")
	fun boolean(): Boolean = value as? Boolean ?: value.toString().toBoolean()
	fun long(): Long = value as? Long ?: value.toString().toLong()

	fun save(path: String)
	{
		val outputFile = Gdx.files.local(path)
		val output = Output(outputFile.write(false))
		save(output)
		output.close()
	}

	fun save(output: Output)
	{
		output.writeString(name)
		output.writeInt(children.size, true)

		if (children.isNotEmpty())
		{
			for (child in children)
			{
				child.save(output)
			}
		}
		else
		{
			when (value)
			{
				is Int ->
				{
					output.writeShort(0)
					output.writeInt(value as Int)
				}
				is Float ->
				{
					output.writeShort(1)
					output.writeFloat(value as Float)
				}
				is Boolean ->
				{
					output.writeShort(2)
					output.writeBoolean(value as Boolean)
				}
				is Long ->
				{
					output.writeShort(4)
					output.writeLong(value as Long)
				}
				else ->
				{
					if (value == null)
					{
						value = ""
					}

					output.writeShort(3)
					output.writeString(value as String)
				}
			}
		}

		output.writeInt(attributeMap.size, true)
		for (att in attributeMap)
		{
			att.value.save(output)
		}
	}

	fun load(path: String)
	{
		load(Gdx.files.internal(path))
	}

	fun load(handle: FileHandle)
	{
		val input = Input(handle.read())
		load(input)
		input.close()
	}

	fun load(input: Input)
	{
		name = input.readString()
		nameId = name.toUpperCase().hashCode()

		val childCount = input.readInt(true)
		children = Array(childCount){ e -> XmlData() }

		if (childCount > 0)
		{
			for (i in 0 until childCount)
			{
				val child = XmlData()
				child.load(input)
				children[i] = child
				childMap[child.nameId] = child
			}
		}
		else
		{
			val valueType = input.readShort().toInt()
			value = when(valueType)
			{
				0 -> input.readInt()
				1 -> input.readFloat()
				2 -> input.readBoolean()
				3 -> input.readString()
				4 -> input.readLong()
				else -> throw RuntimeException("Unknown xml data type '$valueType'!")
			}
		}

		val attCount = input.readInt(true)
		attributeMap = IntMap()
		for (i in 0 until attCount)
		{
			val att = XmlAttributeData()
			att.load(input)
			attributeMap[att.nameId] = att
		}
	}

	companion object
	{
		val cachedXml = ObjectMap<String, XmlData>()
		var existingPaths: com.badlogic.gdx.utils.Array<String>? = null

		fun getXml(path: String): XmlData
		{
			val existing = cachedXml.get(path, null)
			if (existing != null) { return existing }

			var filepath = path

			filepath = filepath.replace("\\", "/")

			filepath = "CompressedData/" + filepath.hashCode() + ".xmldata"

			try
			{
				var handle = Gdx.files.internal(filepath)
				if (!handle.exists()) handle = Gdx.files.absolute(filepath)

				val loaded = XmlData(handle)

				cachedXml[path] = loaded

				return loaded
			}
			catch (ex: Exception)
			{
				throw Exception("Failed to load file: '$path'. Reason: ${ex.message}")
			}
		}

		fun loadFromElement(el: XmlReader.Element): XmlData
		{
			val data = XmlData()
			data.name = el.name
			data.nameId = data.name.toUpperCase().hashCode()

			if (el.childCount == 0 && el.text == null)
			{
				data.value = ""
			}
			else if (el.childCount == 0)
			{
				val intVal = el.text.toIntOrNull()
				if (intVal != null)
				{
					data.value = intVal
				}
				else
				{
					val floatVal = el.text.toFloatOrNull()
					if (floatVal != null)
					{
						data.value = floatVal
					}
					else
					{
						val boolVal = when (el.text.toLowerCase())
						{
							"true" -> true
							"false" -> false
							else -> null
						}
						if (boolVal != null)
						{
							data.value = boolVal
						}
						else
						{
							data.value = el.text
						}
					}
				}
			}
			else
			{
				data.children = Array(el.childCount) { e -> loadFromElement(el.getChild(e)) }
				for (child in data.children)
				{
					data.childMap[child.nameId] = child
				}
			}

			if ((el.attributes?.size ?: 0) > 0)
			{
				data.attributeMap = IntMap()
				for (att in el.attributes)
				{
					val attdata = XmlAttributeData.load(att.key, att.value)
					data.attributeMap[attdata.nameId] = attdata
				}
			}
			else
			{
				data.attributeMap = IntMap()
			}

			return data
		}

		fun getExistingPaths(): Sequence<String>
		{
			if (existingPaths == null)
			{
				existingPaths = com.badlogic.gdx.utils.Array()

				val xml = getXml("ProcessedPaths.xml")
				for (el in xml.children)
				{
					existingPaths?.add(el.text)
				}
			}

			return buildSequence {
				for (path in existingPaths!!)
				{
					yield(path)
				}
			}
		}

		fun enumeratePaths(base: String, type: String): Sequence<String>
		{
			if (existingPaths == null)
			{
				existingPaths = com.badlogic.gdx.utils.Array()

				val xml = getXml("ProcessedPaths.xml")
				for (el in xml.children)
				{
					existingPaths?.add(el.text)
				}
			}

			return buildSequence {
				for (path in existingPaths!!)
				{
					if (path.startsWith(base, true))
					{
						val xml = getXml(path)
						if (xml.name == type)
						{
							yield(path)
						}
					}
				}
			}
		}

		fun performanceTest()
		{
			val runs = 10000

			val rawXml = getRawXml("UnlockTrees/Fire.xml")
			val dataXml = loadFromElement(rawXml)

			dataXml.save("test.xmldata")

			fun profile(lambda: () -> Unit, message: String)
			{
				val start = System.currentTimeMillis()
				for (i in 0..runs)
				{
					lambda.invoke()
				}
				val end = System.currentTimeMillis()
				val diff = end - start

				System.out.println("$message completed in {$diff}ms")
			}

			// get child by name
			profile({rawXml.getChildByName("Abilities")}, "Element.getChildByName")
			profile({dataXml.getChildByName("Abilities")}, "XmlData.getChildByName")

			// get string child
			val elAb = rawXml.getChildByName("Abilities").getChild(0).getChildByName("AbilityData")
			val daAb = dataXml.getChildByName("Abilities")!!.children[0].getChildByName("AbilityData")!!
			profile({elAb.get("UnboughtDescription")}, "Element.get")
			profile({daAb.get("UnboughtDescription")}, "XmlData.get")

			// get int child
			val elEd = elAb.getChildByName("EffectData")
			val daEd = daAb.getChildByName("EffectData")!!
			profile({elEd.getInt("Cost")}, "Element.getInt")
			profile({daEd.getInt("Cost")}, "XmlData.getInt")

			// textload
			profile({getXml("UnlockTrees/Fire.xml")}, "Element.load")
			profile({XmlData().load("test.xmldata")}, "XmlData.load")

			// get string attribute

			// gete int attribute
		}
	}
}

class XmlAttributeData
{
	lateinit var name: String
	var nameId: Int = -1

	var value: Any? = null

	fun text(): String = value.toString()
	fun int(): Int = value as? Int ?: value.toString().toIntOrNull() ?: throw TypeCastException("Cannot cast $value to an Int!")
	fun float(): Float = value as? Float ?: value.toString().toFloatOrNull() ?: throw TypeCastException("Cannot cast $value to a Float!")
	fun boolean(): Boolean = value as? Boolean ?: value.toString().toBoolean()

	fun save(output: Output)
	{
		output.writeString(name)
		when (value)
		{
			is Int ->
			{
				output.writeShort(0)
				output.writeInt(value as Int)
			}
			is Float ->
			{
				output.writeShort(1)
				output.writeFloat(value as Float)
			}
			is Boolean ->
			{
				output.writeShort(2)
				output.writeBoolean(value as Boolean)
			}
			else ->
			{
				output.writeShort(3)
				output.writeString(value as String)
			}
		}
	}

	fun load(input: Input)
	{
		name = input.readString()
		nameId = name.toUpperCase().hashCode()

		val valueType = input.readShort().toInt()
		value = when(valueType)
		{
			0 -> input.readInt()
			1 -> input.readFloat()
			2 -> input.readBoolean()
			3 -> input.readString()
			else -> throw RuntimeException("Unknown xml data type '$valueType'!")
		}
	}

	companion object
	{
		fun load(name: String, rawvalue: String): XmlAttributeData
		{
			val data = XmlAttributeData()
			data.name = name
			data.nameId = name.toUpperCase().hashCode()

			val floatVal = rawvalue.toFloatOrNull()
			if (floatVal != null)
			{
				data.value = floatVal
			}
			else
			{
				val intVal = rawvalue.toIntOrNull()
				if (intVal != null)
				{
					data.value = intVal
				}
				else
				{
					val boolVal = when (rawvalue.toLowerCase())
					{
						"true" -> true
						"false" -> false
						else -> null
					}
					if (boolVal != null)
					{
						data.value = boolVal
					}
					else
					{
						data.value = rawvalue
					}
				}
			}

			return data
		}
	}
}