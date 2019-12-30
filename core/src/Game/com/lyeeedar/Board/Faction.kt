package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Point
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set

/**
 * Created by Philip on 01-Aug-16.
 */

class Faction
{
	val sizeMap = IntMap<Array<MonsterDesc>>()
	val bossSizeMap = IntMap<Array<MonsterDesc>>()

	fun get(size: Int) : MonsterDesc
	{
		var s = size
		while (s > 0)
		{
			if (sizeMap.containsKey(s)) return sizeMap[s].random()
			s--
		}

		return sizeMap.values().first().random()
	}

	fun get(name: String): MonsterDesc?
	{
		for (size in sizeMap)
		{
			for (monster in size.value)
			{
				if (monster.name.toUpperCase() == name.toUpperCase()) return monster
			}
		}

		return null
	}

	fun getBoss(size: Int) : MonsterDesc
	{
		var s = size
		while (s > 0)
		{
			if (bossSizeMap.containsKey(s)) return bossSizeMap[s].random()
			s--
		}

		return bossSizeMap.values().first().random()
	}

	fun getBoss(name: String): MonsterDesc?
	{
		for (size in bossSizeMap)
		{
			for (monster in size.value)
			{
				if (monster.name == name) return monster
			}
		}

		return null
	}

	companion object
	{
		val loadedFactions = ObjectMap<String, Faction>()

		fun load(path: String): Faction
		{
			if (loadedFactions.containsKey(path.toLowerCase()))
			{
				return loadedFactions[path.toLowerCase()]
			}

			val xml = getXml("Factions/$path")

			val faction = Faction()

			val monsterEl = xml.getChildByName("Monsters")!!
			for (i in 0 until monsterEl.childCount)
			{
				val el = monsterEl.getChild(i)
				val desc = MonsterDesc.load(el)

				if (!faction.sizeMap.containsKey(desc.size))
				{
					faction.sizeMap[desc.size] = Array()
				}

				faction.sizeMap[desc.size].add(desc)
			}

			val bossEl = xml.getChildByName("Bosses")
			if (bossEl != null)
			{
				for (i in 0 until bossEl.childCount)
				{
					val el = bossEl.getChild(i)
					val desc = MonsterDesc.load(el)

					if (!faction.bossSizeMap.containsKey(desc.size))
					{
						faction.bossSizeMap[desc.size] = Array()
					}

					faction.bossSizeMap[desc.size].add(desc)
				}
			}

			loadedFactions[path.toLowerCase().replace(".xml", "")] = faction

			return faction
		}

		fun createCustomFaction(level: Level): Faction
		{
			val faction = Faction()
			faction.sizeMap[1] = Array()
			faction.sizeMap[1].add(level.customMonster!!)

			return faction
		}
	}
}

class MonsterDesc
{
	lateinit var name: String
	lateinit var sprite: Sprite
	lateinit var death: ParticleEffect
	var attackNumPips: Int = 5
	var attackCooldown: Point = Point(6, 6)
	var attackDamage: Int = 1
	var size: Int = 1
	var hp: Int = 10
	var damageReduction: Int = 0
	val onTurnEffects = Array<TurnEffect>()
	val abilities = Array<MonsterAbility>()
	val stages = Array<MonsterDesc>()

	var originalDesc: MonsterDesc? = null

	companion object
	{
		fun load(xml: XmlData): MonsterDesc
		{
			val desc = MonsterDesc()

			desc.name = xml.get("Name", "")!!

			desc.sprite = AssetManager.loadSprite(xml.getChildByName("Sprite")!!)
			desc.death = AssetManager.loadParticleEffect(xml.getChildByName("Death")!!).getParticleEffect()

			desc.attackNumPips = xml.getInt("AttackNumPips")

			val atkCooldown = xml.get("AttackCooldown").split(',');
			desc.attackCooldown = Point(atkCooldown[0].toInt(), atkCooldown[1].toInt())

			desc.attackDamage = xml.getInt("AttackDamage", 1)

			desc.size = xml.getInt("Size", 1)

			desc.hp = xml.getInt("HP", 10)

			desc.damageReduction = xml.getInt("DamageReduction", 0)

			val onTurnEffectsEl = xml.getChildByName("TurnEffects")
			if (onTurnEffectsEl != null)
			{
				for (el in onTurnEffectsEl.children)
				{
					desc.onTurnEffects.add(TurnEffect.load(el))
				}
			}

			val abilitiesEl = xml.getChildByName("Abilities")
			if (abilitiesEl != null)
			{
				for (i in 0 until abilitiesEl.childCount)
				{
					val el = abilitiesEl.getChild(i)
					val ability = MonsterAbility.load(el)
					desc.abilities.add(ability)
				}
			}

			val stagesEl = xml.getChildByName("Stages")
			if (stagesEl != null)
			{
				for (stageEl in stagesEl.children)
				{
					val monsterDesc = load(stageEl)
					monsterDesc.name = desc.name
					desc.stages.add(monsterDesc)
					monsterDesc.originalDesc = desc
				}
			}

			return desc
		}
	}
}