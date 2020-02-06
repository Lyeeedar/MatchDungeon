package com.lyeeedar.build.SourceRewriter

import java.io.File

class SourceRewriter(val file: File, val classRegister: ClassRegister)
{
	val variableRegex = "(override |abstract )?(?<VariableType>var|val|lateinit var) (?<Name>[a-zA-Z0-9]*)(: (?<Type>[a-zA-Z0-9<>?]*))?( = (?<DefaultValue>.*))?".toRegex()

	lateinit var originalContents: String
	val dataClasses = ArrayList<XmlDataClassDescription>()

	val fileContents = ArrayList<IFilePart>()

	fun parse(): Boolean
	{
		originalContents = file.readText()

		println("Parsing ${file.absolutePath}")

		val lines = originalContents.split('\n')

		val packagePart = PackageFilePart()
		val importsPart = ImportsFilePart()

		fileContents.add(packagePart)
		fileContents.add(importsPart)

		var currentMiscPart: MiscFilePart? = null
		var currentClassPart: DataClassFilePart? = null
		var funcDepth: Int? = null
		var annotations: ArrayList<String>? = null
		for (line in lines)
		{
			val trimmed = line.trim()
			if (currentClassPart == null)
			{
				if (line.startsWith("package "))
				{
					packagePart.packageStr = line.trim()
					continue
				}
				else if (line.startsWith("import "))
				{
					importsPart.imports.add(line.trim())
					continue
				}
				else if (trimmed.startsWith("class "))
				{
					val name = trimmed.split(':')[0].replace("class ", "").trim()
					val classDefinition = classRegister.classMap[name]
					if (classDefinition?.isXmlDataClass == true)
					{
						currentMiscPart = null

						currentClassPart = DataClassFilePart(classDefinition, classRegister, annotations ?: ArrayList())
						currentClassPart.desc.name = name
						currentClassPart.desc.superClass = trimmed.split(':')[1].trim()
						currentClassPart.desc.classIndentation = line.length - line.trimStart().length
						annotations = null

						dataClasses.add(currentClassPart.desc)

						fileContents.add(currentClassPart)

						continue
					}
				}
				else if (trimmed.startsWith("abstract class "))
				{
					val name = trimmed.split(':')[0].replace("abstract class ", "").trim()
					val classDefinition = classRegister.classMap[name]
					if (classDefinition?.isXmlDataClass == true)
					{
						currentMiscPart = null

						currentClassPart = DataClassFilePart(classDefinition, classRegister, annotations ?: ArrayList())
						currentClassPart.desc.name = name
						currentClassPart.desc.superClass = trimmed.split(':')[1].trim()
						currentClassPart.desc.classIndentation = line.length - line.trimStart().length
						annotations = null

						dataClasses.add(currentClassPart.desc)

						fileContents.add(currentClassPart)

						continue
					}
				}
				else if (trimmed.startsWith("@"))
				{
					if (annotations == null)
					{
						annotations = ArrayList()
					}
					annotations.add(trimmed)

					continue
				}

				if (currentMiscPart == null)
				{
					currentMiscPart = MiscFilePart()
					fileContents.add(currentMiscPart)
				}
				currentMiscPart.code.appendln(line.trimEnd())

				annotations = null
			}
			else
			{
				if (funcDepth != null)
				{
					if (trimmed == "}" && line.trimEnd().length == funcDepth+1)
					{
						funcDepth = null
					}
				}
				else if (trimmed == "{")
				{
					val depth = line.trimEnd().length-1

					if (depth > currentClassPart.desc.classIndentation)
					{
						funcDepth = depth
					}
				}
				else if (trimmed.startsWith("@"))
				{
					if (annotations == null)
					{
						annotations = ArrayList()
					}

					annotations.add(trimmed)
				}
				else
				{
					val matches = variableRegex.matchEntire(trimmed)
					if (matches != null)
					{
						val namedGroups = matches.groups as MatchNamedGroupCollection
						val variableType = when (namedGroups["VariableType"]!!.value)
						{
							"val" -> VariableType.VAL
							"var" -> VariableType.VAR
							"lateinit var" -> VariableType.LATEINIT
							else -> throw RuntimeException("Unknown variable type ${namedGroups["VariableType"]!!.value}")
						}
						val name = namedGroups["Name"]!!.value
						val type = namedGroups["Type"]?.value ?: "String"
						val default = namedGroups["DefaultValue"]?.value ?: ""

						val variableDesc = VariableDescription(variableType, name, type, default, trimmed, annotations ?: ArrayList())
						currentClassPart.desc.variables.add(variableDesc)

						if (variableDesc.variableType == VariableType.VAL && variableDesc.name == "classID")
						{
							currentClassPart.desc.classDefinition.classID = variableDesc.defaultValue
						}

						annotations = null
					}
					else if (trimmed == "}" && line.trimEnd().length == currentClassPart.desc.classIndentation + 1)
					{
						println("Found data class ${currentClassPart.desc.name}")

						currentClassPart = null
						annotations = null
					}
					else
					{
						annotations = null
					}
				}
			}
		}

		return true
	}

	fun write()
	{
		val imports = fileContents[1] as ImportsFilePart
		for (part in fileContents)
		{
			if (part is DataClassFilePart)
			{
				part.desc.resolveImports(imports.imports)
			}
		}

		val output = IndentedStringBuilder()
		for (part in fileContents)
		{
			part.write(output)
		}

		val newContents = output.toString()
		if (newContents != originalContents)
		{
			file.writeText(newContents)

			println("Writing ${file.absolutePath} complete")
		}
		else
		{
			println("Skipping writing ${file.absolutePath}. Identical")
		}
	}
}

interface IFilePart
{
	fun write(builder: IndentedStringBuilder)
}

class PackageFilePart : IFilePart
{
	var packageStr: String = ""

	override fun write(builder: IndentedStringBuilder)
	{
		builder.appendln(packageStr)
		builder.appendln("")
	}
}

class ImportsFilePart : IFilePart
{
	val imports: HashSet<String> = HashSet()

	override fun write(builder: IndentedStringBuilder)
	{
		builder.appendln(imports.sorted().joinToString("\n"))
	}
}

class MiscFilePart : IFilePart
{
	val code: StringBuilder = StringBuilder()

	override fun write(builder: IndentedStringBuilder)
	{
		builder.appendln(code.toString().trimEnd())
	}
}

class DataClassFilePart(classDefinition: ClassDefinition, classRegister: ClassRegister, annotations: ArrayList<String>) : IFilePart
{
	val desc: XmlDataClassDescription = XmlDataClassDescription(classDefinition, classRegister, annotations)

	override fun write(builder: IndentedStringBuilder)
	{
		desc.write(builder)
	}
}

class XmlDataClassDescription(val classDefinition: ClassDefinition, val classRegister: ClassRegister, val annotations: ArrayList<String>)
{
	lateinit var name: String
	lateinit var superClass: String
	var classIndentation = 0
	val variables = ArrayList<VariableDescription>()

	fun resolveImports(imports: HashSet<String>)
	{
		for (variable in variables)
		{
			variable.resolveImports(imports)
		}
	}

	fun write(builder: IndentedStringBuilder)
	{
		for (annotation in annotations)
		{
			builder.appendln(classIndentation, annotation)
		}

		val classType = if (classDefinition.isAbstract) "abstract class" else "class"
		builder.appendln(classIndentation, "$classType $name : $superClass")
		builder.appendln(classIndentation, "{")

		for (variable in variables)
		{
			for (annotation in variable.annotations)
			{
				builder.appendln(classIndentation+1, annotation)
			}

			builder.appendln(classIndentation+1, variable.raw.trimEnd())
			if (variable.name == "classID")
			{
				builder.appendln("")
			}
		}

		builder.appendln("")
		builder.appendln(classIndentation+1, "override fun load(xmlData: XmlData)")
		builder.appendln(classIndentation+1, "{")

		if (classDefinition.superClass != null && classDefinition.superClass!!.name != "XmlDataClass")
		{
			builder.appendln(classIndentation+2, "super.load(xmlData)")
		}

		for (variable in variables)
		{
			variable.writeLoad(builder, classIndentation+2, classRegister)
		}

		builder.appendln(classIndentation+1, "}")

		if (classDefinition.isAbstract)
		{
			builder.appendln("")

			// write switch loader
			builder.appendln(classIndentation+1, "companion object")
			builder.appendln(classIndentation+1, "{")

			builder.appendln(classIndentation+2, "fun loadPolymorphicClass(classID: String): $name")
			builder.appendln(classIndentation+2, "{")

			builder.appendln(classIndentation+3, "return when (classID)")
			builder.appendln(classIndentation+3, "{")

			for (childClass in classDefinition.inheritingClasses)
			{
				if (!childClass.isAbstract)
				{
					builder.appendln(classIndentation+4, "${childClass.classID} -> ${childClass.name}()")
				}
			}

			builder.appendln(classIndentation+4, "else -> throw RuntimeException(\"Unknown classID '\$classID' for $name!\")")
			builder.appendln(classIndentation+3, "}")

			builder.appendln(classIndentation+2, "}")

			builder.appendln(classIndentation+1, "}")
		}

		builder.appendln(classIndentation, "}")
	}
}

enum class VariableType
{
	VAR,
	VAL,
	LATEINIT
}

class VariableDescription(val variableType: VariableType, val name: String, val type: String, val defaultValue: String, val raw: String, val annotations: ArrayList<String>)
{
	fun resolveImports(imports: HashSet<String>)
	{
		if (type == "ParticleEffect" || type == "ParticleEffectDescription" || type == "Sprite")
		{
			imports.add("import com.lyeeedar.Util.AssetManager")
		}
		else if (type == "String" || type == "Int" || type == "Float")
		{

		}
		else
		{

		}
	}

	fun writeLoad(builder: IndentedStringBuilder, indentation: Int, classRegister: ClassRegister)
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
			if (variableType == VariableType.LATEINIT)
			{
				builder.appendln(indentation, "$name = xmlData.get(\"$niceName\")")
			}
			else if (variableType == VariableType.VAR)
			{
				var loadLine = "$name = xmlData.get(\"$niceName\", $defaultValue)"
				if (!nullable)
				{
					loadLine += "!!"
				}
				builder.appendln(indentation, loadLine)
			}
		}
		else if (type == "Int")
		{
			if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "$name = xmlData.getInt(\"$niceName\", $defaultValue)")
			}
		}
		else if (type == "Float")
		{
			if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "$name = xmlData.getFloat(\"$niceName\", $defaultValue)")
			}
		}
		else if (type == "ParticleEffect")
		{
			if (variableType == VariableType.LATEINIT)
			{
				val loadLine = "$name = AssetManager.loadParticleEffect(xmlData.getChildByName(\"$niceName\")!!)"
				builder.appendln(indentation, "${loadLine}.getParticleEffect()")
			}
			else if (variableType == VariableType.VAR)
			{
				var loadLine = "$name = AssetManager.tryLoadParticleEffect(xmlData.getChildByName(\"$niceName\"))"
				if (!nullable)
				{
					loadLine += "!!"
				}
				builder.appendln(indentation, "${loadLine}.getParticleEffect()")
			}
		}
		else if (type == "ParticleEffectDescription")
		{
			if (variableType == VariableType.LATEINIT)
			{
				val loadLine = "$name = AssetManager.loadParticleEffect(xmlData.getChildByName(\"$niceName\")!!)"
				builder.appendln(indentation, loadLine)
			}
			else if (variableType == VariableType.VAR)
			{
				var loadLine = "$name = AssetManager.tryLoadParticleEffect(xmlData.getChildByName(\"$niceName\"))"
				if (!nullable)
				{
					loadLine += "!!"
				}
				builder.appendln(indentation, loadLine)
			}
		}
		else if (type == "Sprite")
		{
			if (variableType == VariableType.LATEINIT)
			{
				val loadLine = "$name = AssetManager.loadSprite(xmlData.getChildByName(\"$niceName\")!!)"
				builder.appendln(indentation, loadLine)
			}
			else if (variableType == VariableType.VAR)
			{
				var loadLine = "$name = AssetManager.tryLoadSprite(xmlData.getChildByName(\"$niceName\"))"
				if (!nullable)
				{
					loadLine += "!!"
				}
				builder.appendln(indentation, loadLine)
			}
		}
		else
		{
			val classDef = classRegister.classMap[type] ?: throw RuntimeException("Unknown type '$type'!")

			val el = name+"El"
			if (variableType == VariableType.LATEINIT || (variableType == VariableType.VAR && !nullable))
			{
				builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$niceName\")!!")

				if (classDef.isAbstract)
				{
					builder.appendln(indentation, "$name = $type.loadPolymorphicClass(${el}.get(\"classID\"))")
				}
				else
				{
					builder.appendln(indentation, "$name = $type()")
				}

				builder.appendln(indentation, "$name.load($el)")
			}
			else if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$niceName\")")
				builder.appendln(indentation, "if ($el != null)")
				builder.appendln(indentation, "{")

				if (classDef.isAbstract)
				{
					builder.appendln(indentation+1, "$name = $type.loadPolymorphicClass(${el}.get(\"classID\"))")
				}
				else
				{
					builder.appendln(indentation+1, "$name = $type()")
				}

				builder.appendln(indentation+1, "$name!!.load($el)")
				builder.appendln(indentation, "}")
			}
			else
			{
				builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$niceName\")!!")
				builder.appendln(indentation, "$name.load($el)")
			}
		}
	}
}