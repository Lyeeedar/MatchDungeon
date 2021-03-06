package com.lyeeedar.build.SourceRewriter

import java.io.File

class ClassRegister(val files: List<File>, val defFolder: File)
{
	val classMap = HashMap<String, ClassDefinition>()
	val interfaceMap = HashMap<String, InterfaceDefinition>()
	val enumMap = HashMap<String, EnumDefinition>()

	fun registerClasses()
	{
		for (file in files)
		{
			parseFile(file)
		}

		for (classDef in classMap.values)
		{
			var didSuperClass = false
			for (inheritFrom in classDef.inheritDeclarations)
			{
				if (!didSuperClass)
				{
					didSuperClass = true

					if (inheritFrom.endsWith(")"))
					{
						val superClass = classMap[inheritFrom.replace("()", "")]
						if (superClass != null)
						{
							classDef.superClass = superClass
						}
					}
					else
					{
						val interfaceDef = interfaceMap[inheritFrom]
						if (interfaceDef != null)
						{
							classDef.interfaces.add(interfaceDef)
						}
					}
				}
				else
				{
					val interfaceDef = interfaceMap[inheritFrom]
					if (interfaceDef != null)
					{
						classDef.interfaces.add(interfaceDef)
					}
				}
			}
		}

		for (classDef in classMap.values)
		{
			classDef.updateParents()
		}

		for (classDef in classMap["XmlDataClass"]!!.inheritingClasses)
		{
			classDef.isXmlDataClass = true
		}
	}

	fun parseFile(file: File)
	{
		val lines = file.readLines()

		var packageStr: String = ""
		for (i in 0 until lines.size)
		{
			val line = lines[i]

			val trimmed = line.trim()

			if (trimmed.startsWith("package "))
			{
				packageStr = trimmed.replace("package ", "")
			}
			else if (trimmed.startsWith("class "))
			{
				val split = trimmed.split(':')

				val name = split[0].replace("class ", "").trim()
				val classDef = ClassDefinition(name)
				classDef.packageStr = packageStr

				if (split.size > 1)
				{
					val inheritsFrom = split[1].split(',')
					for (other in inheritsFrom)
					{
						classDef.inheritDeclarations.add(other.trim())
					}
				}

				classMap.put(name, classDef)
			}
			else if (trimmed.startsWith("abstract class "))
			{
				val split = trimmed.split(':')

				val name = split[0].replace("abstract class ", "").trim()
				val classDef = ClassDefinition(name)
				classDef.isAbstract = true
				classDef.packageStr = packageStr

				if (split.size > 1)
				{
					val inheritsFrom = split[1].split(',')
					for (other in inheritsFrom)
					{
						classDef.inheritDeclarations.add(other.trim())
					}
				}

				classMap.put(name, classDef)
			}
			else if (trimmed.startsWith("interface "))
			{
				val name = trimmed.replace("interface ", "").trim()
				val interfaceDef = InterfaceDefinition(name)
				interfaceDef.packageStr = packageStr

				interfaceMap.put(name, interfaceDef)
			}
			else if (trimmed.startsWith("enum class "))
			{
				val name = trimmed.replace("enum class ", "").trim().split(" ")[0]

				val enumDef = EnumDefinition(name)

				var ii = i+2
				while (true)
				{
					val line = lines[ii]

					val enumValue = line.split(',', '(', ';')[0].trim()
					enumDef.values.add(enumValue)

					if (!line.trim().endsWith(','))
					{
						break
					}
					ii++
				}

				enumMap.put(name, enumDef)
			}
		}
	}

	fun isXmlDataClass(name: String): Boolean
	{
		return classMap[name]!!.isXmlDataClass
	}

	fun writeXmlDefFiles()
	{
		val xmlDataClasses = classMap.values.filter { it.isXmlDataClass }.toList()

		val rootClasses = xmlDataClasses.filter { it.classDef!!.annotations.any { it.name == "DataFile" } }.toHashSet()
		rootClasses.addAll(xmlDataClasses.filter { it.classDef!!.forceGlobal })

		val refCountMap = HashMap<ClassDefinition, Int>()
		for (dataClass in rootClasses)
		{
			fun writeRef(classDef: ClassDefinition)
			{
				val referenced = refCountMap.get(classDef)

				if (referenced == null)
				{
					refCountMap.put(classDef, 0)
				}
				else
				{
					refCountMap.put(classDef, referenced+1)
				}
			}

			for (referencedClass in dataClass.getAllReferencedClasses(HashSet()))
			{
				writeRef(referencedClass)
			}
		}

		if (!defFolder.exists()) defFolder.mkdirs()
		val defFolder = defFolder.absolutePath

		// clean folder
		val sharedFolder = File("$defFolder/Shared")
		for (file in sharedFolder.listFiles()?.filterNotNull() ?: ArrayList())
		{
			file.delete()
		}
		for (file in this.defFolder.listFiles()?.filterNotNull() ?: ArrayList())
		{
			file.delete()
		}

		// write root files
		val writtenSpecificFiles = HashSet<ClassDefinition>()
		for (root in rootClasses)
		{
			val otherClasses = HashSet<ClassDefinition>()
			for (referencedClass in root.getAllReferencedClasses(HashSet()))
			{
				if (rootClasses.contains(referencedClass)) continue

				val refCount = refCountMap[referencedClass] ?: 0
				if (refCount == 0)
				{
					otherClasses.add(referencedClass)
				}
			}

			val dataClassAnnotation = root.classDef!!.annotations.firstOrNull { it.name == "DataFile" } ?: AnnotationDescription("@DataFile()")
			val name = root.classDef!!.dataClassName
			val colour =  dataClassAnnotation.paramMap["colour"]
			val icon = dataClassAnnotation.paramMap["icon"]

			val builder = IndentedStringBuilder()
			val colourLine = if (colour != null) "Colour=\"$colour\"" else ""
			val iconLine = if (icon != null) "Icon=\"$icon\"" else ""
			builder.appendln(0, "<Definitions $colourLine $iconLine xmlns:meta=\"Editor\">")

			if (writtenSpecificFiles.contains(root)) throw RuntimeException("Class written twice!")
			root.classDef!!.createDefFile(builder, false)
			writtenSpecificFiles.add(root)

			for (classDef in otherClasses.sortedBy { it.name })
			{
				if (classDef.classDef!!.forceGlobal) continue

				if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")
				classDef.classDef!!.createDefFile(builder, false)
				writtenSpecificFiles.add(classDef)

				if (classDef.isAbstract)
				{
					val defNames = HashMap<String, ArrayList<String>>()
					for (childDef in classDef.inheritingClasses)
					{
						if (!childDef.isAbstract)
						{
							var category = defNames[childDef.classDef!!.dataClassCategory]
							if (category == null)
							{
								category = ArrayList()
								defNames[childDef.classDef!!.dataClassCategory] = category
							}
							category.add(childDef.classDef!!.dataClassName)
						}
					}
					val keysStr: String
					if (defNames.size == 1)
					{
						keysStr = defNames.values.first().sorted().joinToString(",")
					}
					else
					{
						keysStr = defNames.entries.sortedBy { it.key }.joinToString(",") { "${it.key}(${it.value.sorted().joinToString(",")})" }
					}

					builder.appendln(1, """<Definition Name="${classDef.classDef!!.dataClassName}Defs" Keys="$keysStr" meta:RefKey="ReferenceDef" />""")
				}
			}

			if (root.isAbstract)
			{
				val defNames = HashMap<String, ArrayList<String>>()
				for (childDef in root.inheritingClasses)
				{
					if (!childDef.isAbstract)
					{
						var category = defNames[childDef.classDef!!.dataClassCategory]
						if (category == null)
						{
							category = ArrayList()
							defNames[childDef.classDef!!.dataClassCategory] = category
						}
						category.add(childDef.classDef!!.dataClassName)
					}
				}

				val keysStr: String
				if (defNames.size == 1)
				{
					keysStr = defNames.values.first().sorted().joinToString(",")
				}
				else
				{
					keysStr = defNames.entries.sortedBy { it.key }.joinToString(",") { "${it.key}(${it.value.sorted().joinToString(",")})" }
				}

				builder.appendln(1, """<Definition Name="${root.classDef!!.dataClassName}Defs" Keys="$keysStr" IsGlobal="True" meta:RefKey="ReferenceDef" />""")
			}

			builder.appendln(0, "</Definitions>")
			File("$defFolder/$name.xmldef").writeText(builder.toString())

			println("Created def file $name")
		}

		// write shared files
		val sharedClasses = refCountMap.filter { it.value > 0 }.map { it.key }.toHashSet()

		if (sharedClasses.isNotEmpty())
		{
			File("$defFolder/Shared").mkdirs()

			val sharedClassesToWrite = HashSet<ClassDefinition>()

			for (classDef in sharedClasses)
			{
				if (rootClasses.contains(classDef)) continue

				sharedClassesToWrite.add(classDef)
				if (classDef.isAbstract)
				{
					for (childDef in classDef.inheritingClasses)
					{
						if (rootClasses.contains(childDef)) continue
						sharedClassesToWrite.add(childDef)
					}
				}
			}

			for (abstractClass in sharedClassesToWrite.filter { it.isAbstract && it.superClass!!.superClass == null }.toList())
			{
				val builder = IndentedStringBuilder()
				builder.appendln(0, "<Definitions xmlns:meta=\"Editor\">")

				val defNames = HashMap<String, ArrayList<String>>()

				abstractClass.classDef!!.createDefFile(builder, true)
				sharedClassesToWrite.remove(abstractClass)
				for (classDef in abstractClass.inheritingClasses)
				{
					if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")
					classDef.classDef!!.createDefFile(builder, true)
					sharedClassesToWrite.remove(classDef)

					if (!classDef.isAbstract)
					{
						var category = defNames[classDef.classDef!!.dataClassCategory]
						if (category == null)
						{
							category = ArrayList()
							defNames[classDef.classDef!!.dataClassCategory] = category
						}
						category.add(classDef.classDef!!.dataClassName)
					}
				}
				val keysStr: String
				if (defNames.size == 1)
				{
					keysStr = defNames.values.first().sorted().joinToString(",")
				}
				else
				{
					keysStr = defNames.entries.sortedBy { it.key }.joinToString(",") { "${it.key}(${it.value.sorted().joinToString(",")})" }
				}

				val abstractClassName = abstractClass.classDef!!.dataClassName
				builder.appendln(1, """<Definition Name="${abstractClassName}Defs" Keys="$keysStr" IsGlobal="True" meta:RefKey="ReferenceDef" />""")

				builder.appendln(0, "</Definitions>")
				File("$defFolder/Shared/${abstractClassName}.xmldef").writeText(builder.toString())
				println("Created def file $abstractClassName")
			}

			for (classDef in sharedClassesToWrite)
			{
				if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")

				val builder = IndentedStringBuilder()
				builder.appendln(0, "<Definitions xmlns:meta=\"Editor\">")

				classDef.classDef!!.createDefFile(builder, true)

				builder.appendln(0, "</Definitions>")
				File("$defFolder/Shared/${classDef.classDef!!.dataClassName}.xmldef").writeText(builder.toString())
				println("Created def file ${classDef.classDef!!.dataClassName}")
			}


		}
	}
}

class ClassDefinition(val name: String)
{
	var packageStr: String = ""
	var superClass: ClassDefinition? = null
	val interfaces = ArrayList<InterfaceDefinition>()
	var isAbstract = false

	var inheritDeclarations = ArrayList<String>()

	val inheritingClasses = ArrayList<ClassDefinition>()

	var isXmlDataClass = false
	var classID: String? = null
	var classDef: XmlDataClassDescription? = null
	var referencedClasses = ArrayList<ClassDefinition>()

	fun updateParents(classDef: ClassDefinition? = null)
	{
		if (classDef != null)
		{
			inheritingClasses.add(classDef)
			superClass?.updateParents(classDef)
		}
		else
		{
			superClass?.updateParents(this)
		}
	}

	fun getAllReferencedClasses(processedClasses: HashSet<ClassDefinition>): HashSet<ClassDefinition>
	{
		val output = HashSet<ClassDefinition>()

		output.addAll(referencedClasses)
		output.addAll(inheritingClasses)
		for (classDef in referencedClasses)
		{
			if (!processedClasses.contains(classDef))
			{
				processedClasses.add(classDef)
				output.addAll(classDef.getAllReferencedClasses(processedClasses))
			}
		}
		for (classDef in inheritingClasses)
		{
			output.addAll(classDef.getAllReferencedClasses(processedClasses))
		}

		return output
	}
}

class InterfaceDefinition(val name: String)
{
	var packageStr: String = ""
}

class EnumDefinition(val name: String)
{
	val values = ArrayList<String>()
}