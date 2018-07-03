package com.lyeeedar

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.lyeeedar.Util.AbstractApplicationChanger

/**
 * Created by Philip on 20-Jan-16.
 */
class AndroidApplicationChanger : AbstractApplicationChanger(Gdx.app.getPreferences("game-settings"))
{

	override fun createApplication(game: MainGame, pref: Preferences): Application?
	{
		return null
	}

	override fun processResources()
	{
	}

	override fun updateApplication(pref: Preferences)
	{
		val width = pref.getInteger("resolutionX")
		val height = pref.getInteger("resolutionY")
	}

	override fun setDefaultPrefs(prefs: Preferences)
	{
		prefs.putBoolean("pathfindMovement", false)

		prefs.putFloat("musicVolume", 1f)
		prefs.putFloat("ambientVolume", 1f)
		prefs.putFloat("effectVolume", 1f)

		prefs.putInteger("resolutionX", 360)
		prefs.putInteger("resolutionY", 640)
		prefs.putInteger("fps", 30)
		prefs.putFloat("animspeed", 1f)
	}
}
