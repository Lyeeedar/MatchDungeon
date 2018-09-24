package com.lyeeedar.desktop

import com.lyeeedar.Board.Faction
import com.lyeeedar.Board.Level
import com.lyeeedar.Board.Theme
import com.lyeeedar.Card.Card
import com.lyeeedar.Card.CardContent.CardContent
import com.lyeeedar.Game.Character
import com.lyeeedar.Game.Equipment
import com.lyeeedar.Game.Quest
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Util.XmlData

class XmlLoadTester
{
	companion object
	{
		fun test()
		{
			for (path in XmlData.getExistingPaths().toList())
			{
				try
				{
					val xml = XmlData.getXml(path)
					when (xml.name.toUpperCase())
					{
						"QUEST" -> Quest.Companion.load(path.split("Quests/")[1])
						"CARD" -> Card.Companion.load(path)
						"CARDCONTENT" -> CardContent.load(path)
						"LEVEL" -> Level.load(path)
						"HEAD", "BODY", "MAINHAND", "OFFHAND" -> Equipment.load(path)
						"CHARACTER" -> Character.load(path.split("Characters/")[1])
						"FACTION" -> Faction.load(path.split("Factions/")[1])
						"ORBS", "DATA" -> { }
						"EFFECT" -> ParticleEffect.load(path.split("Particles/")[1])
						"THEME" -> Theme.load(path)
						else -> throw RuntimeException("Unhandled path type '${xml.name}'!")
					}

					System.out.println("Test loaded '$path'")
				}
				catch (ex: Exception)
				{
					System.err.println("Failed to load '$path'")
					throw ex
				}
			}
		}
	}
}