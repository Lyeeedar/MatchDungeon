package com.lyeeedar.Sound

import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml

class SoundInstance
{
	enum class Function
	{
		LINEAR,
		LOG,
		LOGREVERSE,
		INVERSE
	}

	lateinit var sound: Sound
	lateinit var name: String
	lateinit var groupName: String

	var pitch = 1.5f
	var volume = 1f
	var function = Function.LINEAR
	var rangeMin = 2
	var rangeMax = 10

	var shoutFaction: ObjectSet<String>? = null
	var key: String? = null
	var value: Any? = null

	constructor()
	{

	}

	constructor(sound: Sound, group: String)
	{
		this.sound = sound
		this.groupName = group
	}

	fun copy(): SoundInstance
	{
		val soundInstance = SoundInstance()
		soundInstance.sound = sound
		soundInstance.name = name
		soundInstance.groupName = groupName

		soundInstance.pitch = pitch
		soundInstance.volume = volume
		soundInstance.rangeMin = rangeMin
		soundInstance.rangeMax = rangeMax
		soundInstance.function = function

		soundInstance.shoutFaction = shoutFaction
		soundInstance.key = key
		soundInstance.value = value

		return soundInstance
	}

	fun play()
	{
 		sound.play(volume)
	}

	companion object
	{

		@JvmStatic fun load(xml:XmlData): SoundInstance
		{
			val sound = SoundInstance()
			sound.name = xml.get("Name")
			sound.groupName = xml.get("Group")
			sound.sound = AssetManager.loadSound(sound.name)!!

			sound.rangeMin = xml.getInt("RangeMin")
			sound.rangeMax = xml.getInt("RangeMax")
			sound.volume = xml.getFloat("Volume", sound.volume)
			sound.pitch = xml.getFloat("Pitch", sound.pitch)
			sound.function = Function.valueOf(xml.get("Function", "Linear")!!.toUpperCase())

			return sound
		}

		private val soundMap = ObjectMap<String, XmlData>()
		private var loaded = false
		@JvmStatic fun getSound(name: String): SoundInstance
		{
			if (!loaded)
			{
				loaded = true

				val xml: XmlData = getXml("Sounds/SoundMap")
				for (i in 0..xml.childCount - 1)
				{
					val el = xml.getChild(i)
					soundMap.put(el.name, el)
				}
			}

			if (soundMap.containsKey(name))
			{
				return SoundInstance.load(soundMap.get(name))
			}
			else
			{
				val sound = SoundInstance()
				sound.name = name
				sound.sound = AssetManager.loadSound(name)!!
				return sound
			}
		}
	}
}
