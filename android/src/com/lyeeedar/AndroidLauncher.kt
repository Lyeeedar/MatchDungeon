package com.lyeeedar

import android.os.Bundle
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.crashlytics.android.Crashlytics
import com.google.firebase.analytics.FirebaseAnalytics
import com.lyeeedar.MatchDungeon.BuildConfig
import com.lyeeedar.Screens.GameLoopTest
import com.lyeeedar.Util.Statics
import io.fabric.sdk.android.Fabric

class AndroidLauncher : AndroidApplication()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Statics.release = !BuildConfig.DEBUG

		Fabric.with(this, Crashlytics())

		val config = AndroidApplicationConfiguration()
		config.resolutionStrategy.calcMeasures(360, 640)
		config.disableAudio = false

		Statics.android = true
		Statics.crashReporter = AndroidCrashReporter()
		Statics.logger = AndroidLogger()
		//Statics.performanceTracer = AndroidPerformanceTracer()
		Statics.analytics = AndroidAnalytics(FirebaseAnalytics.getInstance(this))
		Statics.game = MainGame()

		initialize(Statics.game, config)

		Statics.applicationChanger = AndroidApplicationChanger()
		Statics.applicationChanger.updateApplication(Statics.applicationChanger.prefs)

		val launchIntent = intent
		if (launchIntent.action == "com.google.intent.action.TEST_LOOP")
		{
			GameLoopTest { finish() }.run()
		}
	}
}
