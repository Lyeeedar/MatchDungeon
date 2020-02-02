package com.lyeeedar.headless

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.AbstractInputStreamContent
import com.google.api.client.http.FileContent
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.androidpublisher.AndroidPublisher
import com.google.api.services.androidpublisher.model.LocalizedText
import com.google.api.services.androidpublisher.model.Track
import com.google.api.services.androidpublisher.model.TrackRelease
import java.io.File
import java.io.FileInputStream
import java.util.*


object AndroidRelease
{
	@JvmStatic fun String.runCommand(): String {
		println(this)
		val output = Runtime.getRuntime().exec(this).inputStream.bufferedReader().readText().trim()
		println(output)
		return output
	}

	@JvmStatic fun uploadToPlaystore(version: String, versionCode: Long, changes: String)
	{
		println("Loading credentials")

		val credential = GoogleCredential.fromStream(FileInputStream("../PrivateStuff/api-7634547800790039050-269564-2e4e30222b69.json"))
			.createScoped(Collections.singleton("https://www.googleapis.com/auth/androidpublisher"))
		credential.expiresInSeconds = 120

		val acquireToken = credential.refreshToken()
		if (!acquireToken)
		{
			throw java.lang.RuntimeException("Unable to acquire access token")
		}

		val releaseFile = File("android/build/outputs/bundle/release/android.aab")
		val packageName = "com.lyeeedar.MatchDungeon"

		println("Beginning edit")

		val service = AndroidPublisher.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), credential).setApplicationName("AndroidReleaseAction").build()
		val editID = service.Edits().insert(packageName, null).execute().id

		println("Uploading aab")

		val aabFile: AbstractInputStreamContent = FileContent("application/octet-stream", releaseFile)
		service.Edits().Bundles().upload(packageName, editID, aabFile).execute()

		println("Assigning to track")
		service.Edits().Tracks().update(packageName, editID, "beta",
										Track().setReleases(
											Collections.singletonList(
												TrackRelease()
													.setName(version)
													.setVersionCodes(Collections.singletonList(versionCode))
													.setStatus("completed")
													.setReleaseNotes(Collections.singletonList(
														LocalizedText()
															.setLanguage("en-GB")
															.setText("Automated nightly release\n\nChanges:\n$changes")))))).execute()

		println("Committing edit")

		service.Edits().commit(packageName, editID).execute()
	}

	@JvmStatic fun main(arg: Array<String>)
	{
		println("Running in: " + File("").absolutePath)

		val changes = "git diff".runCommand()
		if (changes.isBlank())
		{
			throw RuntimeException("No changes in the repo!")
		}

		val lastTag = "git describe --abbrev=0".runCommand().replace("'", "")
		val commitsSinceRelease = ("git log $lastTag..HEAD --oneline").runCommand()

		if (commitsSinceRelease.isNotEmpty()) {
			println("Work to release, continuing")

			val manifestFile = File("android/AndroidManifest.xml")
			val manifestContent = manifestFile.readText()

			val regex = Regex("android:versionName=\"(.*)\"")
			val matches = regex.find(manifestContent)!!
			val version = matches.groupValues[1]

			val regex2 = Regex("android:versionCode=\"(.*)\"")
			val matches2 = regex2.find(manifestContent)!!
			val versionCode = matches2.groupValues[1]

			// push to playstore
			uploadToPlaystore(version, versionCode.toLong(), commitsSinceRelease)

			println("Release complete")
			println("::set-output name=version::$version")
			println("::set-output name=status::Complete")

		} else {
			throw RuntimeException("No commits since last release!")
		}
	}
}