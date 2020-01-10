package com.lyeeedar

import com.lyeeedar.Screens.*
import java.util.*

val DEBUG_SCREEN_OVERRIDE: ScreenEnum? = null

enum class ScreenEnum
{
	GRID,
	QUEST,
	CARD,
	DECK,
	QUESTSELECTION,
	PARTICLEEDITOR,
	TESTCARDLOOK,
	INVALID
}

fun registerDebugScreens(): HashMap<ScreenEnum, AbstractScreen>
{
	val screens = HashMap<ScreenEnum, AbstractScreen>()

	screens[ScreenEnum.PARTICLEEDITOR] = ParticleEditorScreen()
	screens[ScreenEnum.TESTCARDLOOK] = TestCardLookScreen()

	return screens
}

fun registerGameScreens(): HashMap<ScreenEnum, AbstractScreen>
{
	val screens = HashMap<ScreenEnum, AbstractScreen>()

	screens[ScreenEnum.GRID] = GridScreen()
	screens[ScreenEnum.QUEST] = QuestScreen()
	screens[ScreenEnum.CARD] = CardScreen()
	screens[ScreenEnum.DECK] = DeckScreen()
	screens[ScreenEnum.QUESTSELECTION] = QuestSelectionScreen()

	return screens
}