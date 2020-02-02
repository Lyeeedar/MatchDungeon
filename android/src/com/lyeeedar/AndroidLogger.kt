package com.lyeeedar

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.lyeeedar.Util.ILogger

class AndroidLogger : ILogger
{
	val LOG_TAG = "MatchDungeon"

	override fun logDebug(message: String)
	{
		Crashlytics.log(Log.DEBUG, "", message)
		println(message)
		Log.d(LOG_TAG, message)
	}

	override fun logWarning(message: String)
	{
		Crashlytics.log(Log.WARN, "", message)
		Log.w(LOG_TAG, message)
	}

	override fun logError(message: String)
	{
		Crashlytics.log(Log.ERROR, "", message)
		Log.e(LOG_TAG, message)
	}
}