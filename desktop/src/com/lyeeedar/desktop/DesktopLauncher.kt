package com.lyeeedar.desktop

import com.lyeeedar.MainGame
import com.lyeeedar.Util.Statics

object DesktopLauncher
{
	@JvmStatic fun main(arg: Array<String>)
	{
		//Global.release = true
		Statics.game = MainGame()
		Statics.applicationChanger = LwjglApplicationChanger()
		Statics.applicationChanger.createApplication()
	}
}
