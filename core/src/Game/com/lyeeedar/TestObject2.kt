package com.lyeeedar

import com.lyeeedar.Util.DataClass
import com.lyeeedar.Util.DataFile
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.XmlDataClass

@DataFile()
@DataClass(name = "TestObject2")
class TestObject2 : XmlDataClass()
{
	val container: DataContainer = DataContainer()

	override fun load(xmlData: XmlData)
	{
		val containerEl = xmlData.getChildByName("Container")!!
		container.load(containerEl)
	}
}
