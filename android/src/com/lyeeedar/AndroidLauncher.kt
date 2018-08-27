package com.lyeeedar

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.crashlytics.android.Crashlytics
import io.fabric.sdk.android.Fabric

class AndroidLauncher : AndroidApplication()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Fabric.with(this, Crashlytics())

		val config = AndroidApplicationConfiguration()
		config.resolutionStrategy.calcMeasures(360, 640)
		config.disableAudio = false

		Global.android = true
		Global.game = MainGame()

		initialize(Global.game, config)

		Global.applicationChanger = AndroidApplicationChanger()
		Global.applicationChanger.updateApplication(Global.applicationChanger.prefs)
	}
}
