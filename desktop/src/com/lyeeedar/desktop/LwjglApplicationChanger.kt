package com.lyeeedar.desktop

import com.badlogic.gdx.Application
import com.badlogic.gdx.Files.FileType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglPreferences
import com.lyeeedar.MainGame
import com.lyeeedar.Util.AbstractApplicationChanger
import com.lyeeedar.Util.Statics

class LwjglApplicationChanger : AbstractApplicationChanger(LwjglPreferences("game-settings", "settings"))
{
	var prefs: Preferences? = null

	override fun createApplication(game: MainGame, pref: Preferences): Application
	{
		System.setProperty("org.lwjgl.opengl.Window.undecorated", "" + pref.getBoolean("borderless"))

		val cfg = LwjglApplicationConfiguration()

		cfg.title = "Match Dungeon"
		cfg.width = 800
		cfg.height = 600
		cfg.fullscreen = pref.getBoolean("fullscreen")
		cfg.vSyncEnabled = pref.getBoolean("vSync")
		cfg.foregroundFPS = 0
		cfg.backgroundFPS = 0
		cfg.samples = pref.getInteger("msaa")
		cfg.addIcon("Sprites/Unpacked/Icon32.png", FileType.Internal)
		cfg.allowSoftwareMode = true

		Statics.fps = pref.getInteger("fps")

		return LwjglApplication(game, cfg)
	}

	override fun updateApplication(pref: Preferences)
	{
		System.setProperty("org.lwjgl.opengl.Window.undecorated", "" + pref.getBoolean("borderless"))

		val width = pref.getInteger("resolutionX")
		val height = pref.getInteger("resolutionY")
		val fullscreen = pref.getBoolean("fullscreen")

		Statics.fps = pref.getInteger("fps")

		if (fullscreen)
		{
			val mode = Gdx.graphics.displayMode
			Gdx.graphics.setFullscreenMode(mode)
		} else
		{
			Gdx.graphics.setWindowedMode(width, height)
		}
	}

	override fun setDefaultPrefs(prefs: Preferences)
	{
		prefs.putBoolean("pathfindMovement", false)

		prefs.putFloat("musicVolume", 1f)
		prefs.putFloat("ambientVolume", 1f)
		prefs.putFloat("effectVolume", 1f)

		prefs.putInteger("resolutionX", 360)
		prefs.putInteger("resolutionY", 640)
		prefs.putBoolean("fullscreen", false)
		prefs.putBoolean("borderless", false)
		prefs.putBoolean("vSync", true)
		prefs.putInteger("fps", 0)
		prefs.putFloat("animspeed", 1f)
		prefs.putInteger("msaa", 16)
	}

}