package com.lyeeedar

import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.*
import com.lyeeedar.Util.AssetManager








@DataFile()
@DataClass(name = "TestObject")
class TestObjectData : XmlDataClass()
{
	@DataValue(dataName = "Title")
	var name: String = ""
	var description: String = ""
	var count: Int = 0
	lateinit var effect: ParticleEffect
	var icon: Sprite? = null
	lateinit var container: DataContainer

	override fun load(xmlData: XmlData)
	{
		name = xmlData.get("Title", "")!!
		description = xmlData.get("Description", "")!!
		count = xmlData.getInt("Count", 0)
		effect = AssetManager.loadParticleEffect(xmlData.getChildByName("Effect")!!).getParticleEffect()
		icon = AssetManager.tryLoadSprite(xmlData.getChildByName("Icon"))
		val containerEl = xmlData.getChildByName("Container")!!
		container = DataContainer()
		container.load(containerEl)
	}
}

class DataContainer : XmlDataClass()
{
	@NumericRange(min = 2f, max = 99f)
	var fraction: Float = 10f
	lateinit var node: AbstractNode
	var optionalNode: AbstractNode? = null

	override fun load(xmlData: XmlData)
	{
		fraction = xmlData.getFloat("Fraction", 10f)
		val nodeEl = xmlData.getChildByName("Node")!!
		node = AbstractNode.loadPolymorphicClass(nodeEl.get("classID"))
		node.load(nodeEl)
		val optionalNodeEl = xmlData.getChildByName("OptionalNode")
		if (optionalNodeEl != null)
		{
			optionalNode = AbstractNode.loadPolymorphicClass(optionalNodeEl.get("classID"))
			optionalNode!!.load(optionalNodeEl)
		}
	}
}

@DataClass(name = "Nodes")
abstract class AbstractNode : XmlDataClass()
{
	abstract val classID: String
	
	lateinit var description: String
	
	
	companion object
	
	
	companion object
	
	
	companion object
	
	
	companion object
	
	
	companion object
	
	
	companion object
	
	
	companion object
	
	
	companion object
	
	
	companion object

	override fun load(xmlData: XmlData)
	{
		description = xmlData.get("Description")
	}

	companion object
	{
		fun loadPolymorphicClass(classID: String): AbstractNode
		{
			return when (classID)
			{
				"String" -> StringNode()
				"Int" -> IntNode()
				else -> throw RuntimeException("Unknown classID '$classID' for AbstractNode!")
			}
		}
	}
}

@DataClass(name = "String", category = "Actions")
class StringNode : AbstractNode()
{
	override val classID = "String"
	
	lateinit var text: String

	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		text = xmlData.get("Text")
	}
}

@DataClass(name = "Int", category = "Descriptors")
class IntNode : AbstractNode()
{
	override val classID = "Int"
	
	var value: Int = 5

	override fun load(xmlData: XmlData)
	{
		super.load(xmlData)
		value = xmlData.getInt("Value", 5)
	}
}









