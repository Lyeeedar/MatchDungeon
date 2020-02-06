package com.lyeeedar.build.SourceRewriter

import java.io.File

class ClassRegister(val files: List<File>)
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