package com.lyeeedar.headless

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object AndroidRelease
{
	@JvmStatic fun getAccessToken(): String
	{
		val credential = GoogleCredential.fromStream(FileInputStream("../PrivateStuff/api-7634547800790039050-269564-2e4e30222b69.json"))
			.createScoped(Collections.singleton("https://www.googleapis.com/auth/androidpublisher"))

		val success = credential.refreshToken()

		if (!success)
		{
			throw java.lang.RuntimeException("Unable to acquire access token")
		}

		println(credential.accessToken)

		return credential.accessToken
	}

	@JvmStatic fun String.runCommand(): String {
		println(this)
		val output = Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
		println(output)
		return output
	}

	data class ResponseAndCode(val response: String, val code: Int)
	@JvmStatic fun uploadToPlaystore(version: String): ResponseAndCode
	{
		val releaseFile = File("android/build/outputs/bundle/release/android.aab")
		val aabBytes = releaseFile.readBytes()
		val accessToken = getAccessToken()

		println("Beginning upload to playstore")
		val url = URL("https://www.googleapis.com/upload/androidpublisher/v3/applications/com.lyeeedar/edits/$version/bundles?uploadType=media")
		with (url.openConnection() as HttpURLConnection) {
			requestMethod = "POST"
			doOutput = true
			doInput = true
			setRequestProperty("Content-Type", "application/octet-stream")
			setRequestProperty("Authorization", "Bearer $accessToken")

			outputStream.write(aabBytes)
			outputStream.flush()

			println("URL : $url")
			println("Response Code : $responseCode")

			inputStream.bufferedReader().use {
				val response = StringBuffer()

				var inputLine = it.readLine()
				while (inputLine != null) {
					response.append(inputLine)
					inputLine = it.readLine()
				}
				it.close()

				println("Response : $response")

				return ResponseAndCode(response.toString(), responseCode)
			}
		}
	}

	@JvmStatic fun main(arg: Array<String>)
	{
		println("Running in: " + File("").absolutePath)

		val lastTag = "git describe --tags --abbrev=0".runCommand()
		val commitsSinceRelease = ("git log $lastTag..HEAD --oneline").runCommand()

		if (!commitsSinceRelease.isNullOrEmpty()) {
			println("Work to release, continuing")

			val manifestFile = File("android/AndroidManifest.xml")
			val manifestContent = manifestFile.readText()

			val regex = Regex("android:versionName=\"(.*)\"")
			val matches = regex.find(manifestContent)!!
			val version = matches.groupValues[0]

			// push to playstore
			val responseAndCode = uploadToPlaystore(version)

			if (responseAndCode.code == 200) {

				// commit changes
				"git add .".runCommand()
				"git commit -m\"Bump version number and release\"".runCommand()
				("git tag -a releases/$version -m \"Release $version\"").runCommand()

				println("Release complete")
			} else {
				println("Something went wrong with releasing! Got code: " + responseAndCode.code)
				throw RuntimeException("Something went wrong with releasing! Got code: " + responseAndCode.code)
			}

		} else {
			println("Release up to date")
		}
	}
}