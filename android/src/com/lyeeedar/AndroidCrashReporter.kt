package com.lyeeedar

import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import com.lyeeedar.Util.ICrashReporter
import com.lyeeedar.Util.ITrace

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

	override fun getTrace(name: String): ITrace
	{
		val trace = FirebasePerformance.getInstance().newTrace(name)

		return AndroidTrace(trace)
	}
}

class AndroidTrace(val trace: Trace) : ITrace
{
	override fun start()
	{
		trace.start()
	}

	override fun stop()
	{
		trace.stop()
	}

}