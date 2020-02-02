package com.lyeeedar.desktop

import com.badlogic.gdx.Gdx
import com.lyeeedar.MainGame
import com.lyeeedar.Util.Statics

object GameLoopTest
{
	@JvmStatic fun main(arg: Array<String>)
	{
		Statics.game = MainGame()
		Statics.applicationChanger = LwjglApplicationChanger()
		Statics.applicationChanger.createApplication()

		com.lyeeedar.Screens.GameLoopTest { Gdx.app.exit() }.run()
	}
}