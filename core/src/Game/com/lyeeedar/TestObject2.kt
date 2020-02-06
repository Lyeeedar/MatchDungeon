package com.lyeeedar

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass
import com.lyeeedar.Util.XmlDataFile

@XmlDataFile(name = "TestObject2")
class TestObject2 : XmlDataClass()
{
	val container: DataContainer = DataContainer()

	override fun load(xmlData: XmlData)
	{
		val containerEl = xmlData.getChildByName("Container")!!
		container.load(containerEl)
	}
}

