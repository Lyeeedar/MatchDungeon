package com.lyeeedar.build.SourceRewriter

import java.io.File

class ClassRegister(val files: List<File>, val defFolder: File)
{
	val classMap = HashMap<String, ClassDefinition>()
	val interfaceMap = HashMap<String, InterfaceDefinition>()

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
		for (line in lines)
		{
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
		}
	}

	fun isXmlDataClass(name: String): Boolean
	{
		return classMap[name]!!.isXmlDataClass
	}

	fun writeXmlDefFiles()
	{
		val xmlDataClasses = classMap.values.filter { it.isXmlDataClass }.toList()

		val rootClasses = xmlDataClasses.filter { it.classDef!!.annotations.any { it.name == "XmlDataFile" } }.toList()

		val refCountMap = HashMap<ClassDefinition, Int>()
		for (dataClass in xmlDataClasses)
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

			for (referencedClass in dataClass.referencedClasses)
			{
				writeRef(referencedClass)

				if (referencedClass.isAbstract)
				{
					for (childClass in referencedClass.inheritingClasses)
					{
						writeRef(childClass)
					}
				}
			}
		}

		val defFolder = defFolder.absolutePath

		// clean folder
		for (file in this.defFolder.listFiles())
		{
			file.delete()
		}

		// write root files
		val writtenSpecificFiles = HashSet<ClassDefinition>()
		for (root in rootClasses)
		{
			val otherClasses = HashSet<ClassDefinition>()
			for (referencedClass in root.referencedClasses)
			{
				if (rootClasses.contains(referencedClass)) continue

				val refCount = refCountMap[referencedClass] ?: 0
				if (refCount == 0)
				{
					otherClasses.add(referencedClass)

					if (referencedClass.isAbstract)
					{
						for (childClass in referencedClass.inheritingClasses)
						{
							if (rootClasses.contains(childClass)) continue

							val refCount = refCountMap[referencedClass] ?: 0
							if (refCount == 0)
							{
								otherClasses.add(referencedClass)
							}
						}
					}
				}
			}

			val dataClassAnnotation = root.classDef!!.annotations.first { it.name == "XmlDataFile" }
			val name = dataClassAnnotation.paramMap["name"] ?: root.classDef!!.name
			val colour =  dataClassAnnotation.paramMap["colour"]
			val icon = dataClassAnnotation.paramMap["icon"]

			val builder = IndentedStringBuilder()
			val colourLine = if (colour != null) "Colour=$colour" else ""
			val iconLine = if (icon != null) "Icon=$icon" else ""
			builder.appendln(0, "<Definitions $colourLine $iconLine xmlns:meta=\"Editor\">")

			if (writtenSpecificFiles.contains(root)) throw RuntimeException("Class written twice!")
			root.classDef!!.createDefFile(builder, false)
			writtenSpecificFiles.add(root)

			for (classDef in otherClasses)
			{
				if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")
				classDef.classDef!!.createDefFile(builder, false)
				writtenSpecificFiles.add(classDef)
			}

			builder.appendln(0, "</Definitions>")
			File("$defFolder/$name.xmldef").writeText(builder.toString())

			println("Created def file $name")
		}

		// write shared files
		val sharedClasses = refCountMap.filter { it.value > 0 }.map { it.key }.toList()

		if (sharedClasses.isNotEmpty()) {
			val sharedClassesToWrite = HashSet<ClassDefinition>()
			val builder = IndentedStringBuilder()
			builder.appendln(0, "<Definitions xmlns:meta=\"Editor\">")

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

			for (classDef in sharedClassesToWrite)
			{
				if (writtenSpecificFiles.contains(classDef)) throw RuntimeException("Class written twice!")
				classDef.classDef!!.createDefFile(builder, true)
			}

			builder.appendln(0, "</Definitions>")
			File("$defFolder/Shared.xmldef").writeText(builder.toString())
			println("Created def file Shared")
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
}

class InterfaceDefinition(val name: String)
{
	var packageStr: String = ""
}