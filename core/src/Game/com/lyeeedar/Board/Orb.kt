package com.lyeeedar.Board

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.getXml
import ktx.collections.set

/**
 * Created by Philip on 04-Jul-16.
 */

class Orb(desc: OrbDesc, theme: Theme): Matchable(theme)
{
	override var desc: OrbDesc = OrbDesc()
		set(value)
		{
			field = value
			sprite = desc.sprite.copy()
			sprite.colour = desc.sprite.colour
		}

	init
	{
		this.desc = desc
	}

	override val canMatch: Boolean
		get() = true

	override var markedForDeletion: Boolean = false
	override var deletionEffectDelay: Float = 0f
	var skipPowerOrb = false

	override val canMove: Boolean
		get() = !sealed

	override fun toString(): String
	{
		return desc.key.toString()
	}

	fun setAttributes(orb: Orb)
	{
		sealCount = orb.sealCount
	}

	companion object
	{
		// ----------------------------------------------------------------------
		private val validOrbs: Array<OrbDesc> = Array()
		private val namedOrbs = ObjectMap<String, OrbDesc>()

		fun isNamedOrb(orbDesc: OrbDesc) = namedOrbs.containsKey(orbDesc.name)

		fun getNamedOrb(name: String) = namedOrbs[name]

		fun getRandomOrb(level: Level, toIgnore: OrbDesc? = null): OrbDesc
		{
			if (toIgnore != null)
			{
				while (true)
				{
					val index = MathUtils.random(level.orbs - 1)
					if (validOrbs[index] == toIgnore) continue
					return validOrbs[index]
				}
			}
			else
			{
				val index = MathUtils.random(level.orbs - 1)
				return validOrbs[index]
			}
		}

		fun getValidOrbs(level: Level) : Array<OrbDesc>
		{
			val copy = Array<OrbDesc>(level.orbs)
			for (i in 0 until level.orbs) copy.add(validOrbs[i])
			return copy
		}

		fun getOrb(key: Int) = validOrbs.first { it.key == key }
		fun getOrb(name: String) = validOrbs.first { it.name == name }

		init
		{
			loadOrbs()
		}

		fun loadOrbs()
		{
			validOrbs.clear()
			namedOrbs.clear()

			val xml = getXml("Orbs/Orbs")

			val template = xml.getChildByName("Template")!!
			val baseSprite = AssetManager.loadSprite(template.getChildByName("Sprite")!!)
			val deathEffect = AssetManager.loadParticleEffect(template.getChildByName("Death")!!).getParticleEffect()

			val types = xml.getChildByName("Types")!!
			for (i in 0 until types.childCount)
			{
				val type = types.getChild(i)
				val name = type.get("Name")
				val colour = AssetManager.loadColour(type.getChildByName("Colour")!!)

				val orbDesc = OrbDesc()
				orbDesc.sprite = baseSprite.copy()
				orbDesc.sprite.colour = colour
				orbDesc.name = name

				orbDesc.death = deathEffect
				orbDesc.death.colour = colour

				validOrbs.add(orbDesc)
			}

			val namedOrbsEl = xml.getChildByName("NamedOrbs")
			if (namedOrbsEl != null)
			{
				for (orbEl in namedOrbsEl.children)
				{
					val name = orbEl.get("Name")
					val sprite = AssetManager.loadSprite(orbEl.getChildByName("Sprite")!!)
					val death = AssetManager.loadParticleEffect(orbEl.getChildByName("Death")!!).getParticleEffect()

					val orbDesc = OrbDesc()
					orbDesc.name = name
					orbDesc.sprite = sprite
					orbDesc.death = death
					orbDesc.isNamed = true

					namedOrbs[name] = orbDesc
				}
			}
		}
	}
}

class OrbDesc()
{
	constructor(sprite: Sprite, death: ParticleEffect, key: Int, name: String, isNamed: Boolean) : this()
	{
		this.sprite = sprite
		this.death = death
		this.name = name
		this.key = key
		this.isNamed = isNamed
	}

	var sprite: Sprite = AssetManager.loadSprite("white")
	var death: ParticleEffect = AssetManager.loadParticleEffect("Death").getParticleEffect()
	var key: Int = -1
	var isNamed: Boolean = false
	var name: String = ""
		set(value)
		{
			field = value
			key = value.hashCode()
		}
}