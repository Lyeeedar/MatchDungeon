package com.lyeeedar.build.SourceRewriter

import java.io.File

class SourceRewriter(val file: File)
{
	val variableRegex = "(var|val|lateinit var) ([a-zA-Z0-9]*): ([a-zA-Z0-9<>?]*) = (.*)".toRegex()

	fun rewrite()
	{
		val source = file.readText()
		if (!source.contains("XmlDataClass")) return

		println("Rewriting ${file.absolutePath}")

		val lines = source.split('\n')

		val output = StringBuilder()
		var packageStr = ""
		val imports = HashSet<String>()
		var currentClass: XmlDataClassDescription? = null
		for (line in lines)
		{
			val trimmed = line.trim()
			if (currentClass == null)
			{
				if (trimmed.startsWith("class") && trimmed.endsWith(" : XmlDataClass"))
				{
					currentClass = XmlDataClassDescription()
					currentClass.name = trimmed.replace("class", "").replace(": XmlDataClass", "").trim()
					currentClass.classIndentation = line.length - line.trimStart().length
				}
				else if (line.startsWith("package "))
				{
					packageStr = line.trim()
				}
				else if (line.startsWith("import "))
				{
					imports.add(line.trim())
				}
				else if (imports.size != 0)
				{
					output.appendln(line.trimEnd())
				}
			}
			else
			{
				val matches = variableRegex.matchEntire(trimmed)
				if (matches != null)
				{
					val variableType = VariableType.valueOf(matches.groupValues[1].toUpperCase())
					val name = matches.groupValues[2]
					val type = matches.groupValues[3]
					val default = matches.groupValues[4]

					val variableDesc = VariableDescription(variableType, name, type, default, trimmed)
					currentClass.variables.add(variableDesc)
				}
				else if (trimmed == "}" && line.trimEnd().length == currentClass.classIndentation+1)
				{
					println("Found data class ${currentClass.name}")

					currentClass.write(output, imports)
					currentClass = null
				}
			}
		}

		val newSource = "$packageStr\n\n${imports.sorted().joinToString("\n")}\n\n${output.trim()}"
		if (newSource != source)
		{
			file.writeText(newSource)

			println("Rewrite complete")
		}
		else
		{
			println("Rewrite identical")
		}
	}
}

class XmlDataClassDescription()
{
	lateinit var name: String
	var classIndentation = 0
	val variables = ArrayList<VariableDescription>()

	fun write(builder: StringBuilder, imports: HashSet<String>)
	{
		val classIndentation = "\t".repeat(classIndentation)
		val contentIndentation = "\t".repeat(this.classIndentation+1)

		builder.appendln("${classIndentation}class $name : XmlDataClass")
		builder.appendln("$classIndentation{")

		for (variable in variables)
		{
			builder.appendln("$contentIndentation${variable.raw}")
		}

		builder.appendln("")
		builder.appendln("${contentIndentation}override fun load(xmlData: XmlData)")
		builder.appendln("$contentIndentation{")

		val loadIndentation = "\t".repeat(this.classIndentation+2)
		for (variable in variables)
		{
			variable.writeLoad(builder, imports, loadIndentation)
		}

		builder.appendln("$contentIndentation}")

		builder.appendln("$classIndentation}")
	}
}

enum class VariableType
{
	VAR,
	VAL,
	LATEINIT
}

class VariableDescription(val variableType: VariableType, val name: String, val type: String, val defaultValue: String, val raw: String)
{
	fun writeLoad(builder: StringBuilder, imports: HashSet<String>, indentation: String)
	{
		val niceName = name.capitalize()
		var type = type
		var nullable = false
		if (type.endsWith('?'))
		{
			type = type.substring(0, type.length-1)
			nullable = true
		}

		if (type == "String")
		{
			var loadLine = "$name = xmlData.get(\"$niceName\", $defaultValue)"
			if (!nullable)
			{
				loadLine += "!!"
			}
			builder.appendln("$indentation$loadLine")
		}
		else if (type == "Int")
		{
			builder.appendln("${indentation}$name = xmlData.getInt(\"$niceName\", $defaultValue)")
		}
		else if (type == "Float")
		{
			builder.appendln("${indentation}$name = xmlData.getFloat(\"$niceName\", $defaultValue)")
		}
		else if (type == "ParticleEffect")
		{
			imports.add("import com.lyeeedar.Util.AssetManager")

			var loadLine = "$name = AssetManager.tryLoadParticleEffect(xmlData.getChildByName(\"$niceName\"))"
			if (!nullable)
			{
				loadLine += "!!"
			}
			builder.appendln("$indentation${loadLine}.getParticleEffect()")
		}
		else if (type == "ParticleEffectDescription")
		{
			imports.add("import com.lyeeedar.Util.AssetManager")

			var loadLine = "$name = AssetManager.tryLoadParticleEffect(xmlData.getChildByName(\"$niceName\"))"
			if (!nullable)
			{
				loadLine += "!!"
			}
			builder.appendln("$indentation${loadLine}")
		}
		else if (type == "Sprite")
		{
			imports.add("import com.lyeeedar.Util.AssetManager")

			var loadLine = "$name = AssetManager.tryLoadSprite(xmlData.getChildByName(\"$niceName\"))"
			if (!nullable)
			{
				loadLine += "!!"
			}
			builder.appendln("$indentation${loadLine}")
		}
		else
		{
			throw RuntimeException("Unknown variable type: $type")
		}
	}
}