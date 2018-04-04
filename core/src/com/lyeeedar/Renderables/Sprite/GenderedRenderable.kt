package com.lyeeedar.Renderables.Sprite

import com.lyeeedar.Gender
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.FastEnumMap
import com.lyeeedar.Util.XmlData

class GenderedRenderable
{
	val renderables = FastEnumMap<Gender, DirectionalSprite>(Gender::class.java)

	companion object
	{
		fun parse(xml: XmlData): GenderedRenderable
		{
			val gr = GenderedRenderable()
			for (el in xml.children())
			{
				val gender = Gender.valueOf(el.name.toUpperCase())
				val content = AssetManager.loadDirectionalSprite(el)

				gr.renderables[gender] = content
			}

			return gr
		}
	}
}