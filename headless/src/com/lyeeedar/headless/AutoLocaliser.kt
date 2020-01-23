package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.translate.Translate
import com.google.cloud.translate.TranslateOptions
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.children
import com.lyeeedar.Util.getRawXml
import com.lyeeedar.Util.getXml
import ktx.collections.addAll
import ktx.collections.set
import ktx.collections.toGdxArray
import org.mockito.Mockito
import java.io.File
import java.io.FileInputStream
import java.util.*


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
			Localiser().autoLocalise()
		}
		finally
		{
			Gdx.app.exit()
		}
	}
}

class Localiser
{
	lateinit var translate: Translate

	val languages = arrayOf("EN-US", "DE")
	val translatedText = ObjectMap<String, String>()

	val dictionary = ObjectSet<String>()
	val slang = ObjectMap<String, String>()
	val gameNames = Array<String>()

	val apostraphedWords = arrayOf("you'", "it'", "we'", "won'", "can'")
	val pluralisers = arrayOf("s", "'s", "ies", "es")
	val yers = arrayOf("y", "ly", "ily", "ingly")
	val eers = arrayOf("en", "ed", "er")
	val others = arrayOf("ing", "n't", "ion", "able", "ish")

	init
	{
		dictionary.addAll(File("../assetsraw/Localisation/Dictionaries/GeneralDictionary.txt").readLines())
		dictionary.addAll(File("../assetsraw/Localisation/Dictionaries/GameDictionary.txt").readLines())
		gameNames.addAll(File("../assetsraw/Localisation/Dictionaries/GameNameList.txt").readLines())

		for (slangRaw in File("../assetsraw/Localisation/Dictionaries/SlangDictionary.txt").readLines())
		{
			val split = slangRaw.split(':')
			val slangWord = split[0]
			val actualWord = split[1]

			slang[slangWord] = actualWord
			dictionary.add(slangWord)
		}

		println("Dictionaries loaded")
	}

	fun autoLocalise()
	{
		validateEnglish()

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
				val id = el.getAttribute("ID")
				val text = el.text
				val hashCode = text.hashCode()
				english.add(LocEntry(id, text, "", hashCode))
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
						languageEntry.text = translateText(english, language)
						languageEntry.translateType = "Auto"
						languageEntry.translatedHash = englishHash
					}

					translatedText["$language@@@$english"] = languageEntry.text

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

	fun translateText(text: String, language: String): String
	{
		if (language == "EN-US")
		{
			return text
		}

		val key = "$language@@@$text"
		if (translatedText.containsKey(key))
		{
			val text = translatedText[key]
			println("Loading translation from cache")

			return text
		}

		if (text.contains("{"))
		{
			val splitText = text.split('{', '}')

			var translatedText = ""
			var isSentence = text[0] != '{'
			for (chunk in splitText)
			{
				if (isSentence)
				{
					translatedText += doTranslate(chunk, language)
				}
				else
				{
					translatedText += "{$chunk}"
				}

				isSentence = !isSentence
			}

			return translatedText
		}
		else
		{
			return doTranslate(text, language)
		}
	}

	fun doTranslate(text: String, language: String): String
	{
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

	fun validateEnglish()
	{
		println("")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("#####      Localisation Validator      #######")
		println("")
		println("-------------------------------------------------------------------------")
		println("")
		println("")

		val allIds = ObjectSet<String>()
		for (xml in XmlData.getExistingPaths())
		{
			val xml = getXml(xml)
			for (el in xml.descendants())
			{
				if (el.childCount == 0)
				{
					allIds.add(el.text)
				}
			}
		}

		val allRequiredIds = ObjectSet<String>()

		fun parseCodeFilesRecursive(dir: File)
		{
			val contents = dir.listFiles() ?: return

			for (file in contents)
			{
				if (file.isDirectory)
				{
					parseCodeFilesRecursive(file)
				}
				else
				{
					val contents = file.readText()
					val regex = Regex("Localisation.getText\\(\".*?\"")

					val occurances = regex.findAll(contents)

					for (occurance in occurances)
					{
						var id = occurance.value
						id = id.replace("Localisation.getText(\"", "")
						id = id.replace("\"", "")

						allIds.add(id)
						allRequiredIds.add(id)
					}
				}
			}
		}
		parseCodeFilesRecursive(File("../../core/src").absoluteFile)

		println("ID's found")

		val allFoundIds = ObjectSet<String>()

		val englishLocFolder = File("../assetsraw/Localisation/EN-GB")
		for (file in englishLocFolder.listFiles()!!)
		{
			println("-----------------------------------------------------")
			println("Processing file ${file.name}")

			var unusedLines = ""
			val missingWords = ObjectSet<String>()

			val englishFile = getRawXml(file.path)
			for (el in englishFile.children())
			{
				val id = el.getAttribute("ID")
				val text = el.text

				if (allFoundIds.contains(id))
				{
					throw RuntimeException("Duplicate id found!\nID: $id\nText: $text\nFile: ${file.path}")
				}
				allFoundIds.add(id)

				try
				{
					spellCheck(text, "ID: $id\nText: $text\nFile: ${file.path}")
				}
				catch (ex: Exception)
				{
					val message = ex.message ?: ""
					if (message.contains("Unknown word"))
					{
						val word = message.split('\n')[1].replace("Word: ", "")
						missingWords.add(word)

						System.err.println(message)
					}
					else
					{
						throw ex
					}
				}

				if (!allIds.contains(id))
				{
					unusedLines += id + "\n"
				}
			}

			if (missingWords.size > 0)
			{
				var joined = ""
				for (word in missingWords)
				{
					joined += "$word\n"
				}
				throw RuntimeException("The following words were missing:\n$joined")
			}
			if (!unusedLines.isBlank())
			{
				throw RuntimeException("Loc file $file contained unreferences lines:\n$unusedLines")
			}
		}

		for (id in allRequiredIds)
		{
			if (!allFoundIds.contains(id))
			{
				throw RuntimeException("Required id $id not found!")
			}
		}

		println("Validated English")
	}

	fun spellCheck(text: String, context: String)
	{
		val isTitle = !(text.contains('.') || text.contains(','))

		if (!isTitle && text.last().isLetterOrDigit())
		{
			throw RuntimeException("Text did not end in a full stop!\n$context")
		}

		val sentences = text.split('.', ':', '?', '!', '"').map { it.trim() }.filter { it.isNotEmpty() }.toGdxArray()
		for (sentence in sentences)
		{
			val sentenceContext = "Sentence: $sentence\n$context"

			if (sentence[0].isLetter() && !sentence[0].isUpperCase())
			{
				throw RuntimeException("Sentence did not start with an uppercase letter.\n$sentenceContext")
			}

			var sentenceSimple = sentence
			for (name in gameNames)
			{
				sentenceSimple = sentenceSimple.replace(name, "---")
			}

			val words = sentenceSimple.split(' ')
			var firstWord = true
			outer@ for (word in words)
			{
				if (word == "---") continue

				var lowerWord = word.toLowerCase(Locale.ENGLISH).filter { it.isLetterOrDigit() || it == '\'' }

				if (lowerWord.startsWith('\''))
				{
					lowerWord = lowerWord.substring(1)
				}
				if (lowerWord.endsWith('\''))
				{
					lowerWord = lowerWord.substring(0, lowerWord.length-1)
				}

				val wordContext = "Word: $lowerWord\n$sentenceContext"

				if (lowerWord == "i" || lowerWord.startsWith("i'"))
				{
					if (!word.filter { it.isLetterOrDigit() }[0].isUpperCase())
					{
						throw RuntimeException("$word not uppercase\n$wordContext")
					}
				}
				else if (!firstWord && !isTitle && word[0].isLetter() && word[0].isUpperCase() && dictionary.contains(lowerWord) && !word.all { it.isUpperCase() })
				{
					throw RuntimeException("Word capitalised when it shouldnt be\n$wordContext")
				}
				else if (!dictionary.contains(lowerWord))
				{
					if (apostraphedWords.any { lowerWord.startsWith(it) })
					{
						continue
					}
					for (suffix in pluralisers)
					{
						if (lowerWord.endsWith(suffix))
						{
							val root = lowerWord.substring(0, lowerWord.length-suffix.length)

							if (dictionary.contains(root) || dictionary.contains(root + "y") || dictionary.contains(root + "e"))
							{
								continue@outer
							}
						}
					}
					for (suffix in yers)
					{
						if (lowerWord.endsWith(suffix))
						{
							val root = lowerWord.substring(0, lowerWord.length-suffix.length)

							if (dictionary.contains(root) || dictionary.contains(root + "e"))
							{
								continue@outer
							}
						}
					}
					for (suffix in eers)
					{
						if (lowerWord.endsWith(suffix))
						{
							if (dictionary.contains(lowerWord.substring(0, lowerWord.length-1)))
							{
								continue@outer
							}
							if (dictionary.contains(lowerWord.substring(0, lowerWord.length-2)))
							{
								continue@outer
							}
						}
					}
					for (suffix in others)
					{
						if (lowerWord.endsWith(suffix))
						{
							val root = lowerWord.substring(0, lowerWord.length-suffix.length)

							if (dictionary.contains(root) || dictionary.contains(root + "e"))
							{
								continue@outer
							}
						}
					}

					if (lowerWord.toIntOrNull() != null)
					{
						val asNum = lowerWord.toInt()
						if (!isTitle && asNum < 10)
						{
							throw RuntimeException("Low number as digits in a sentence\n$wordContext")
						}

						continue
					}

					throw RuntimeException("Unknown word\n$wordContext")
				}

				firstWord = false
			}
		}
	}
}