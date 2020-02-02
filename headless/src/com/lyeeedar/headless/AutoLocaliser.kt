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
import org.languagetool.AnalyzedSentence
import org.languagetool.JLanguageTool
import org.languagetool.language.BritishEnglish
import org.languagetool.rules.Rule
import org.languagetool.rules.RuleMatch
import org.languagetool.rules.patterns.PatternRule
import org.languagetool.rules.spelling.SpellingCheckRule
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
	val algorithmVersion = 1

	lateinit var translate: Translate

	val languages = Array<String>()
	val translatedText = ObjectMap<String, String>()

	val slang = ObjectMap<String, String>()
	val gameWords = Array<String>()

	init
	{
		val languagesXml = getRawXml("../assetsraw/Localisation/Languages.xml")
		for (el in languagesXml.children())
		{
			val code = el.get("Code")
			if (code != "en")
			{
				languages.add(code)
			}
		}
		println("Found languages " + languages.joinToString(", "))

		gameWords.addAll(File("../assetsraw/Localisation/Dictionaries/GameWords.txt").readLines())

		for (slangRaw in File("../assetsraw/Localisation/Dictionaries/SlangDictionary.txt").readLines())
		{
			val split = slangRaw.split(':')
			val slangWord = split[0]
			val actualWord = split[1]

			slang[slangWord] = actualWord
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

		val englishLocFolder = File("../assetsraw/Localisation/en")
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

	fun translateText(rawText: String, language: String): String
	{
		val key = "$language@@@$rawText"
		if (translatedText.containsKey(key))
		{
			val text = translatedText[key]
			println("Loading translation from cache")

			return text
		}

		val text = removeSlang(rawText)

		if (text.contains("{"))
		{
			val splitText = text.split('{', '}')

			var simplifiedText = ""
			val variables = Array<String>()

			var isSentence = text[0] != '{'
			for (chunk in splitText)
			{
				if (isSentence)
				{
					simplifiedText += chunk
				}
				else
				{
					simplifiedText += "{${variables.size}}"
					variables.add("{$chunk}")
				}

				isSentence = !isSentence
			}

			var translatedText = doTranslate(simplifiedText, language)
			for (i in 0 until variables.size)
			{
				translatedText = translatedText.replace("{$i}", variables[i])
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

	lateinit var languageTool: JLanguageTool
	lateinit var languageToolMinimal: JLanguageTool
	fun initialiseLanguageAnalyser()
	{
		languageTool = JLanguageTool(BritishEnglish())
		languageTool.addRule(DefinedNameRule(gameWords.filter { it[0].isUpperCase() }.toGdxArray()))
		for (rule in languageTool.allActiveRules)
		{
			if (rule is SpellingCheckRule)
			{
				rule.addIgnoreTokens(gameWords.filter { it.split(' ').size == 1 }.toList())
			}
			else if (rule is PatternRule)
			{
				if (rule.message.startsWith("Use a smart opening quote here"))
				{
					languageTool.disableRule(rule.id)
				}
				else if (rule.message.startsWith("Use a smart closing quote here"))
				{
					languageTool.disableRule(rule.id)
				}
			}
			else if (rule.id == "RUDE_SARCASTIC" || rule.id == "PROFANITY")
			{
				languageTool.disableRule(rule.id)
			}
		}

		languageToolMinimal = JLanguageTool(BritishEnglish())
		for (rule in languageToolMinimal.allActiveRules)
		{
			if (rule is SpellingCheckRule)
			{
				rule.addIgnoreTokens(gameWords.filter { it.split(' ').size == 1 }.toList())
			}
			else
			{
				languageToolMinimal.disableRule(rule.id)
			}
		}
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

		initialiseLanguageAnalyser()

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

		val englishLocFolder = File("../assetsraw/Localisation/en")
		for (file in englishLocFolder.listFiles()!!)
		{
			println("-----------------------------------------------------")
			println("Processing file ${file.name}")

			var unusedLines = ""
			val missingWords = ObjectSet<String>()

			val fileContentsHash = ("algorithm:$algorithmVersion\n" + file.readText()).hashCode()
			val cacheFile = File("../caches/loc-en-${file.nameWithoutExtension}")
			val needsSentenceAnalysis = if (cacheFile.exists()) cacheFile.readText().toInt() != fileContentsHash else true

			if (!needsSentenceAnalysis)
			{
				println("File found identical in cache, skipping sentence analysis")
			}

			val englishFile = getRawXml(file.path)
			var i = 2
			for (el in englishFile.children())
			{
				val id = el.getAttribute("ID")

				if (el.text == null)
				{
					throw RuntimeException("ID had no content!\nID: $id\nFile: ${file.path}")
				}
				val text = el.text

				if (allFoundIds.contains(id))
				{
					throw RuntimeException("Duplicate id found!\nID: $id\nText: $text\nFile: ${file.path}")
				}
				allFoundIds.add(id)

				if (needsSentenceAnalysis)
				{
					val context = "ID: $id\nText: $text\nFile: ${file.path}\nLine: $i"
					doSentenceAnalysis(text, context)

					val lastChar = text.last()
					val endsInPunctuation = lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '"'
					if (id.contains(".Title") || id.contains(".Name"))
					{
						if (endsInPunctuation)
						{
							throw RuntimeException("Title/Name ends with a full stop!\n$context")
						}
					}
					else if (id.contains(".Choice"))
					{
						if (endsInPunctuation)
						{
							throw RuntimeException("Choice ends with a full stop!\n$context")
						}
					}
					else if (id.contains(".Description") || id.contains("Line"))
					{
						if (!endsInPunctuation)
						{
							throw RuntimeException("Sentence does not end with a full stop!\n$context")
						}
					}
				}

				if (!allIds.contains(id))
				{
					unusedLines += id + "\n"
				}

				i++
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

			cacheFile.writeText(fileContentsHash.toString())
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

	fun removeSlang(rawText: String): String
	{
		val splitSentence = splitSentence(rawText)
		for (i in 0 until splitSentence.size)
		{
			if (slang.containsKey(splitSentence[i]))
			{
				splitSentence[i] = slang[splitSentence[i]]
			}
		}
		val text = splitSentence.joinToString("")
		return text
	}

	fun doSentenceAnalysis(rawText: String, context: String)
	{
		var useMinimal = false
		if (rawText.all { it.isLetterOrDigit() || it == ' ' })
		{
			val words = rawText.split(' ')
			if (words.all { it[0].isUpperCase() })
			{
				// this is a title
				useMinimal = true
			}
			else if (words.size < 4)
			{
				// this is a short thing
				useMinimal = true
			}
		}

		val text = removeSlang(rawText)

		val matches = if (useMinimal) languageToolMinimal.check(text) else languageTool.check(text)

		if (matches.size > 0)
		{
			System.err.println("########################################################\n")
			System.err.println(context)
			System.err.println("")

			var failed = false
			for (match in matches)
			{
				System.err.println(text.substring(match.fromPos, match.toPos))
				System.err.println(match.message)
				System.err.println(match.suggestedReplacements)
				System.err.println("\n-----------------------------------------------------\n")

				if (match.message.startsWith("Possible agreement error") && match.message.contains("(Some collective nouns can be treated as both singular and plural, so 'are' is not always incorrect.)"))
				{
					// lets ignore this one
				}
				else
				{
					failed = true
				}
			}

			if (failed) throw RuntimeException("Check failed")
		}
	}

	fun splitSentence(text: String): Array<String>
	{
		val output = Array<String>()
		var currentChunk = ""
		for (i in text.indices)
		{
			val currentChar = text[i]
			if (currentChar.isLetterOrDigit())
			{
				currentChunk += currentChar
			}
			else
			{
				if (!currentChunk.isBlank()) output.add(currentChunk)
				currentChunk = ""
				output.add(currentChar.toString())
			}
		}
		if (!currentChunk.isBlank()) output.add(currentChunk)

		return output
	}
}

class DefinedNameRule(val names: Array<String>) : Rule()
{
	override fun getId(): String
	{
		return "definedNameRule"
	}

	override fun match(sentence: AnalyzedSentence): kotlin.Array<RuleMatch>
	{
		val matches = Array<RuleMatch>()

		for (name in names)
		{
			val lower = name.toLowerCase(Locale.ENGLISH)
			if (sentence.text.contains(lower))
			{
				val start = sentence.text.indexOf(lower)
				matches.add(RuleMatch(this, sentence, start, start + name.length, "Did you mean [$name]?"))
			}
		}

		return matches.toArray(RuleMatch::class.java)
	}

	override fun getDescription(): String
	{
		return "Defined names must always appear with the same case"
	}

}