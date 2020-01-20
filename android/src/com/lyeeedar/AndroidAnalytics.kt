package com.lyeeedar

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.lyeeedar.Util.IAnalytics
import com.lyeeedar.Util.IBundle

class AndroidAnalytics(val firebaseAnalytics: FirebaseAnalytics) : IAnalytics<AndroidBundle>
{
	override fun appOpen()
	{
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, Bundle())
	}

	override fun levelStart(levelName: Long)
	{
		val bundle = Bundle()
		bundle.putLong(FirebaseAnalytics.Param.LEVEL_NAME, levelName)
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_START, bundle)
	}

	override fun levelEnd(levelName: Long, success: String)
	{
		val bundle = Bundle()
		bundle.putLong(FirebaseAnalytics.Param.LEVEL_NAME, levelName)
		bundle.putString(FirebaseAnalytics.Param.SUCCESS, success)
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LEVEL_END, bundle)
	}

	override fun selectContent(type: String, id: String)
	{
		val bundle = Bundle()
		bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, type)
		bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id)
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle)
	}

	override fun tutorialBegin()
	{
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_BEGIN, Bundle())
	}

	override fun tutorialEnd()
	{
		firebaseAnalytics.logEvent(FirebaseAnalytics.Event.TUTORIAL_COMPLETE, Bundle())
	}

	override fun getParamBundle(): AndroidBundle
	{
		return AndroidBundle(Bundle())
	}

	override fun customEvent(name: String, paramBundle: AndroidBundle)
	{

		firebaseAnalytics.logEvent(name, paramBundle.bundle)
	}
}

class AndroidBundle(val bundle: Bundle) : IBundle
{
	override fun setString(key: String, value: String)
	{
		bundle.putString(key, value)
	}

	override fun setInt(key: String, value: Int)
	{
		bundle.putInt(key, value)
	}

	override fun setBool(key: String, value: Boolean)
	{
		bundle.putBoolean(key, value)
	}

}