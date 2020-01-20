package com.lyeeedar

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.lyeeedar.Util.ILogger

class AndroidLogger : ILogger
{
	override fun logDebug(message: String)
	{
		Crashlytics.log(Log.DEBUG, "", message)
	}

	override fun logWarning(message: String)
	{
		Crashlytics.log(Log.WARN, "", message)
	}

	override fun logError(message: String)
	{
		Crashlytics.log(Log.ERROR, "", message)
	}
}