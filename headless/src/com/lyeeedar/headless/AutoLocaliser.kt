package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.lyeeedar.Util.children
import com.lyeeedar.Util.getRawXml
import ktx.collections.set
import org.mockito.Mockito
import java.io.File
import java.io.FileInputStream

class LocEntry(val id: String, var text: String, var translateType: String, var translatedHash: Int)

object AutoLocaliser
{
	@JvmStatic fun main(arg: kotlin.Array<String>)
	{
		Gdx.gl = Mockito.mock(GL20::class.java)
		Gdx.gl20 = Mockito.mock(GL20::class.java)
		Gdx.app = HeadlessApplication(Mockito.mock(Game::class.java))

		try
		{
			Localiser()
		}
		finally
		{
			Gdx.app.exit()
		}
	}
}

class Localiser
{
	val translate: Translate

	val languages = arrayOf("EN-US", "DE")

	init
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Auto Localiser      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		val credentials = GoogleCredentials.fromStream(FileInputStream("../../../PrivateStuff/google-translate-d52258605bea.json"))
		val translateOptions = TranslateOptions.newBuilder().setCredentials(credentials).build()
		translate = translateOptions.service
		println("Translate service started")

		val englishLocFolder = File("../assetsraw/Localisation/EN-GB")

		for (file in englishLocFolder.listFiles()!!)
		{
			println("-----------------------------------------------------")
			println("Processing file ${file.name}")

			val english = Array<LocEntry>()

			val englishFile = getRawXml(file.path)
			for (el in englishFile.children())
			{
				english.add(LocEntry(el.getAttribute("ID"), el.text, "", el.text.hashCode()))
			}

			for (language in languages)
			{
				println("")
				println("Processing language $language")
				println("")

				val languageFilePath = "../assetsraw/Localisation/${language}/${file.name}"

				val contents = ObjectMap<String, LocEntry>()

				if (File(languageFilePath).exists())
				{
					val languageFile = getRawXml(languageFilePath)

					for (el in languageFile.children())
					{
						val id = el.getAttribute("ID")
						val translateType = el.getAttribute("TranslateType")
						val translatedHash = el.getAttribute("TranslatedHash").toInt()
						val text = el.text

						contents[id] = LocEntry(id, text, translateType, translatedHash)
					}
				}

				var outputXml = "<Localisation Language=\"${language}\" xmlns:meta=\"Editor\">\n"

				for (line in english)
				{
					val id = line.id
					val english = line.text
					val englishHash = line.translatedHash

					val languageEntry: LocEntry
					if (contents.containsKey(id))
					{
						languageEntry = contents[id]
					}
					else
					{
						languageEntry = LocEntry(id, "", "None", -1)
					}

					if (languageEntry.translatedHash != englishHash)
					{
						languageEntry.text = translateText(language, english)
						languageEntry.translateType = "Auto"
						languageEntry.translatedHash = englishHash
					}

					outputXml += "\t<Text ID=\"${id}\" TranslateType=\"${languageEntry.translateType}\" TranslatedHash=\"${languageEntry.translatedHash}\">${languageEntry.text}</Text>\n"
				}

				outputXml += "</Localisation>"

				val file = File(languageFilePath)
				file.parentFile.mkdirs()
				file.writeText(outputXml)
			}
		}

		println("Localisation complete")
	}

	fun translateText(language: String, text: String): String
	{
		if (language == "EN-US")
		{
			return text
		}

		println("Translating '${text}' for language $language")

		// do google translate
		val translation = translate.translate(
			text,
			Translate.TranslateOption.sourceLanguage("en"),
			Translate.TranslateOption.targetLanguage(language.toLowerCase()),
			Translate.TranslateOption.model("base"))

		println("Received translation: ${translation.translatedText}")
		println("")

		return translation.translatedText
	}
}