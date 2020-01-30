package com.lyeeedar

import com.lyeeedar.Screens.*
import com.lyeeedar.Util.Statics
import java.util.*

val DEBUG_SCREEN_OVERRIDE: ScreenEnum? = null

enum class ScreenEnum
{
	NEWUSER,
	GRID,
	QUEST,
	CARD,
	DECK,
	QUESTSELECTION,
	QUESTSELECTION2,
	PARTICLEEDITOR,
	TESTCARDLOOK,
	INVALID
}

fun registerDebugScreens(): HashMap<ScreenEnum, AbstractScreen>
{
	val screens = HashMap<ScreenEnum, AbstractScreen>()

	if (!Statics.android)
	{
		screens[ScreenEnum.PARTICLEEDITOR] = ParticleEditorScreen()
	}

	screens[ScreenEnum.TESTCARDLOOK] = TestCardLookScreen()
	screens[ScreenEnum.QUESTSELECTION2] = QuestSelection2Screen()

	return screens
}

fun registerGameScreens(): HashMap<ScreenEnum, AbstractScreen>
{
	val screens = HashMap<ScreenEnum, AbstractScreen>()

	screens[ScreenEnum.NEWUSER] = NewUserScreen()
	screens[ScreenEnum.GRID] = GridScreen()
	screens[ScreenEnum.QUEST] = QuestScreen()
	screens[ScreenEnum.CARD] = CardScreen()
	screens[ScreenEnum.DECK] = DeckScreen()
	screens[ScreenEnum.QUESTSELECTION] = QuestSelectionScreen()

	return screens
}