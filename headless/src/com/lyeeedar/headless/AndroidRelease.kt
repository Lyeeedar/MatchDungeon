package com.lyeeedar.headless

import com.badlogic.gdx.utils.JsonReader
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStreamWriter
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
	@JvmStatic fun doHttpRequest(urlStr: String, accessToken: String, setter: (connection: HttpURLConnection) -> Unit): ResponseAndCode
	{
		val url = URL(urlStr)
		with (url.openConnection() as HttpURLConnection) {
			doOutput = true
			doInput = true
			setRequestProperty("Authorization", "Bearer $accessToken")
			setter(this)

			println("URL : $url")
			println("Response Code : $responseCode")
			println("Response Message: $responseMessage")

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


	@JvmStatic fun uploadToPlaystore(version: String)
	{
		val releaseFile = File("android/build/outputs/bundle/release/android.aab")
		val aabBytes = releaseFile.readBytes()
		val accessToken = getAccessToken()

		val packageName = "com.lyeeedar"

		println("Beginning upload to playstore")

		val beginEditRequest = doHttpRequest("https://www.googleapis.com/androidpublisher/v3/applications/$packageName/edits", accessToken) {
			it.requestMethod = "POST"
			it.setRequestProperty("Content-Type", "application/json")

			val requestJson = """
|{
	"id": "releaseAction-$version",
	"expiryTimeSeconds": "${(Date().time/1000)+120}"
| }""".trimMargin()

			println(requestJson)

			val wr = OutputStreamWriter(it.outputStream)
			wr.write(requestJson)
			wr.flush()
		}

		if (beginEditRequest.code != HttpURLConnection.HTTP_OK)
		{
			throw RuntimeException("Failed to begin edit!")
		}
		val editId = JsonReader().parse(beginEditRequest.response).getString("id")

		val uploadRequest = doHttpRequest("https://www.googleapis.com/upload/androidpublisher/v3/applications/$packageName/edits/$editId/bundles?uploadType=media", accessToken) {
			it.connectTimeout = 120000
			it.requestMethod = "POST"
			it.setRequestProperty("Content-Type", "application/octet-stream")
			it.setRequestProperty("Content-Length", aabBytes.size.toString())

			val stream = DataOutputStream(it.outputStream)
			stream.write(aabBytes)
			stream.flush()
		}

		if (uploadRequest.code != HttpURLConnection.HTTP_OK)
		{
			throw RuntimeException("Failed to upload aab!")
		}

		val commitRequest = doHttpRequest("https://www.googleapis.com/androidpublisher/v3/applications/$packageName/edits/$editId:commit", accessToken) {
			it.requestMethod = "POST"
		}

		if (commitRequest.code != HttpURLConnection.HTTP_OK)
		{
			throw RuntimeException("Failed to commit change!")
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
			val version = matches.groupValues[1]

			// push to playstore
			uploadToPlaystore(version)

			// commit changes
			"git add .".runCommand()
			"git commit -m\"Bump version number and release\"".runCommand()
			("git tag -a releases/$version -m \"Release $version\"").runCommand()

			println("Release complete")

		} else {
			println("Release up to date")
		}
	}
}