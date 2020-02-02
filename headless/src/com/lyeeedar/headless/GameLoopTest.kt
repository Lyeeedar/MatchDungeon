package com.lyeeedar.headless

object GameLoopTest
{
	@JvmStatic fun String.runCommand(): String {
		println(this)
		val output = Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
		println(output)
		return output
	}

	fun readLogs(androidHome: String, pid: String, completeLog: StringBuilder)
	{
		val logs = "$androidHome/adb logcat -d".runCommand().split('\n')
		for (log in logs)
		{
			completeLog.append(log)
			if (log.contains(" $pid ")) println(log)
		}

		"$androidHome/adb logcat -c".runCommand()
	}

	@JvmStatic fun main(arg: Array<String>)
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Beginning Game Loop Test      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		val androidHome = System.getenv("ANDROID_HOME") + "/platform-tools"
		println("ANDROID_HOME: $androidHome")

		"$androidHome/adb install android/build/outputs/apk/debug/android-debug.apk".runCommand()
		"$androidHome/adb shell am start -a com.google.intent.action.TEST_LOOP -n com.lyeeedar.MatchDungeon/com.lyeeedar.AndroidLauncher -S".runCommand()
		"$androidHome/adb logcat -c".runCommand()

		var pid = ""
		while (pid.isBlank())
		{
			pid = "$androidHome/adb shell pidof com.lyeeedar.MatchDungeon".runCommand()
			Thread.sleep(1000) // 1 seconds
		}

		val completeLogs = StringBuilder()
		while ("$androidHome/adb shell pidof com.lyeeedar.MatchDungeon".runCommand().isNotBlank())
		{
			readLogs(androidHome, pid, completeLogs)
			Thread.sleep(5000) // 5 seconds
		}
		readLogs(androidHome, pid, completeLogs)

		var crash = ""

		var inCrash = false
		for (line in completeLogs.lines())
		{
			if (line.startsWith("--------- beginning of crash") || line.contains("FATAL EXCEPTION: GLThread"))
			{
				inCrash = true
			}
			else if (inCrash)
			{
				if (line.contains(" E "))
				{
					crash += line.split(" E ")[1] + "\n"
				}
				else
				{
					break
				}
			}
		}

		if (crash.isNotBlank())
		{
			throw RuntimeException(crash)
		}

		println("")
		println("#####      Game Loop Test Complete      #######")
		println("-------------------------------------------------------------------------")
	}
}