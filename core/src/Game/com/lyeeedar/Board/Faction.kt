package com.lyeeedar.Board

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Util.getXml
import com.lyeeedar.Util.random
import com.lyeeedar.Util.randomOrNull
import ktx.collections.set
import java.util.*

/**
 * Created by Philip on 01-Aug-16.
 */

class Faction
{
	val sizeMap = IntMap<Array<MonsterDesc>>()
	val bossSizeMap = IntMap<Array<MonsterDesc>>()

	fun get(size: Int, grid: Grid) : MonsterDesc
	{
		var s = size
		while (s > 0)
		{
			if (sizeMap.containsKey(s)) return sizeMap[s].asSequence().random(grid.ran)!!
			s--
		}

		return sizeMap.values().first().random(grid.ran)
	}

	fun get(name: String): MonsterDesc?
	{
		for (size in sizeMap)
		{
			for (monster in size.value)
			{
				if (monster.name.toUpperCase(Locale.ENGLISH) == name.toUpperCase(Locale.ENGLISH)) return monster
			}
		}

		return null
	}

	fun getBoss(size: Int, grid: Grid) : MonsterDesc
	{
		var s = size
		while (s > 0)
		{
			if (bossSizeMap.containsKey(s)) return bossSizeMap[s].asSequence().random(grid.ran)!!
			s--
		}

		return bossSizeMap.values().firstOrNull()?.randomOrNull(grid.ran) ?: get(size, grid)
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
			if (loadedFactions.containsKey(path.toLowerCase(Locale.ENGLISH)))
			{
				return loadedFactions[path.toLowerCase(Locale.ENGLISH)]
			}

			val xml = getXml("Factions/$path")

			val faction = Faction()

			val monsterEl = xml.getChildByName("Monsters")!!
			for (i in 0 until monsterEl.childCount)
			{
				val el = monsterEl.getChild(i)
				val desc = MonsterDesc()
				desc.load(el)

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
					val desc = MonsterDesc()
					desc.load(el)

					if (!faction.bossSizeMap.containsKey(desc.size))
					{
						faction.bossSizeMap[desc.size] = Array()
					}

					faction.bossSizeMap[desc.size].add(desc)
				}
			}

			loadedFactions[path.toLowerCase(Locale.ENGLISH).replace(".xml", "")] = faction

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

