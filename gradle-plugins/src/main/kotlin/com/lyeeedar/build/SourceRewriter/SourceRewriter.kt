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

		val lines = originalContents.split('\n')

		val packagePart = PackageFilePart()
		val importsPart = ImportsFilePart()

		fileContents.add(packagePart)
		fileContents.add(importsPart)

		var currentMiscPart: MiscFilePart? = null
		var currentClassPart: DataClassFilePart? = null
		var funcDepth: Int? = null
		var annotations: ArrayList<AnnotationDescription>? = null
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

						currentClassPart = DataClassFilePart(
							name,
							trimmed.split(':')[1].trim(),
							line.length - line.trimStart().length,
							classDefinition,
							classRegister,
							annotations ?: ArrayList())
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

						currentClassPart = DataClassFilePart(
							name,
							trimmed.split(':')[1].trim(),
							line.length - line.trimStart().length,
							classDefinition,
							classRegister,
							annotations ?: ArrayList())
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
					annotations.add(AnnotationDescription(trimmed))

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

					annotations.add(AnnotationDescription(trimmed))
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
		if (!fileContents.any { it is DataClassFilePart }) return

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