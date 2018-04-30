package com.lyeeedar.Board

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Board.CompletionCondition.CompletionConditionDie
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.getXml
import ktx.collections.set

/**
 * Created by Philip on 04-Jul-16.
 */

class Orb(desc: OrbDesc, theme: Theme): Swappable(theme)
{
	var desc: OrbDesc = OrbDesc()
		get() = field
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

	var armed: ((point: Point, grid: Grid, orb: Orb) -> Unit)? = null

	var special: Special? = null
		set(value)
		{
			field = value

			if (value != null)
			{
				val nsprite = value.sprite.copy()
				nsprite.colour = sprite.colour
				if (nsprite.colourAnimation == null) nsprite.colourAnimation = BlinkAnimation.obtain().set(nsprite.colour, 0.1f, 2.5f, false)

				sprite = nsprite
			}
		}

	var markedForDeletion: Boolean = false
	var deletionEffectDelay: Float = 0f
	var skipPowerOrb = false

	var delayDisplayAttack: Float = 0f
	var hasAttack: Boolean = false
		set(value)
		{
			if (!field && value)
			{
				field = value
				val nsprite = AssetManager.loadSprite("Oryx/uf_split/uf_items/skull_small", drawActualSize = true)
				nsprite.colour = sprite.colour
				sprite = nsprite

				val grid = this.grid!!
				if (grid.level.defeatConditions.filter { it is CompletionConditionDie }.isEmpty())
				{
					val die = CompletionConditionDie()
					die.attachHandlers(grid)
					GridScreen.instance.defeatTable.add(die.createTable(grid))
					GridScreen.instance.defeatTable.row()

					grid.level.defeatConditions.add(die)
				}
			}
		}

	var attackTimer = 0

	var isChanger: Boolean = false
	var nextDesc: OrbDesc? = null
		set(value)
		{
			field = value
			nextSprite = sprite.copy()
			nextSprite!!.colour = nextDesc!!.sprite.colour.copy().a(0.75f)
			nextSprite!!.baseScale[0] = 1.25f
			nextSprite!!.baseScale[1] = 1.25f
		}
	var nextSprite: Sprite? = null

	val key: Int
		get() = if (special is Match5) -1 else desc.key

	override val canMove: Boolean
		get() = !sealed && armed == null

	override fun toString(): String
	{
		return desc.key.toString()
	}

	fun setAttributes(orb: Orb)
	{
		sealCount = orb.sealCount
		hasAttack = orb.hasAttack
		attackTimer = orb.attackTimer
		special = orb.special
	}

	companion object
	{
		// ----------------------------------------------------------------------
		private val validOrbs: Array<OrbDesc> = Array()
		private val namedOrbs = ObjectMap<String, OrbDesc>()

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
			val deathEffect = AssetManager.loadParticleEffect(template.getChildByName("Death")!!)

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
					val death = AssetManager.loadParticleEffect(orbEl.getChildByName("Death")!!)

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

	lateinit var sprite: Sprite
	lateinit var death: ParticleEffect
	var key: Int = -1
	var isNamed: Boolean = false
	var name: String = ""
		set(value)
		{
			field = value
			key = value.hashCode()
		}
}