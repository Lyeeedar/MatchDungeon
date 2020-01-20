package com.lyeeedar

import com.crashlytics.android.Crashlytics
import com.lyeeedar.Util.ICrashReporter

class AndroidCrashReporter : ICrashReporter
{
	override fun setCustomKey(key: String, value: String)
	{
		Crashlytics.setString(key, value)
	}

	override fun setCustomKey(key: String, value: Boolean)
	{
		Crashlytics.setBool(key, value)
	}

	override fun setCustomKey(key: String, value: Double)
	{
		Crashlytics.setDouble(key, value)
	}

	override fun setCustomKey(key: String, value: Float)
	{
		Crashlytics.setFloat(key, value)
	}

	override fun setCustomKey(key: String, value: Int)
	{
		Crashlytics.setInt(key, value)
	}
}