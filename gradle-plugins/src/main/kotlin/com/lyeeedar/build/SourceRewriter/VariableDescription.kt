package com.lyeeedar.build.SourceRewriter

enum class VariableType
{
    VAR,
    VAL,
    LATEINIT
}

class VariableDescription(val variableType: VariableType, val name: String, val type: String, val defaultValue: String, val raw: String, val annotations: ArrayList<AnnotationDescription>)
{
	val dataName: String
	var visibleIfStr: String = ""

	init
	{
		val dataValueAnnotation = annotations.firstOrNull { it.name == "DataValue" }
		if (dataValueAnnotation != null)
		{
			dataName = dataValueAnnotation.paramMap["dataName"]?.replace("\"", "") ?: name.capitalize()

			val visibleIfRaw = dataValueAnnotation.paramMap["visibleIf"]
			if (visibleIfRaw != null)
			{
				visibleIfStr = """VisibleIf="$visibleIfRaw""""
			}
		}
		else
		{
			dataName = name.capitalize()
		}
	}

    fun resolveImports(imports: HashSet<String>, classDefinition: ClassDefinition, classRegister: ClassRegister)
    {
		var type = type
		var nullable = false
		if (type.endsWith('?'))
		{
			type = type.substring(0, type.length-1)
			nullable = true
		}

        if (type == "ParticleEffect" || type == "ParticleEffectDescription" || type == "Sprite" || type == "SpriteWrapper")
        {
            imports.add("import com.lyeeedar.Util.AssetManager")
        }
        else if (type == "String" || type == "Int" || type == "Float" || type == "Boolean")
        {
            // primitives dont need imports
        }
		else if (type == "Point" || type == "Vector2" || type == "Vector3"|| type == "Vector4")
		{

		}
		else if (classRegister.enumMap.containsKey(type))
		{
			imports.add("import java.util.*")
		}
		else if (type.startsWith("Array<"))
		{
			val arrayType = type.replace("Array<", "").dropLast(1)
			if (arrayType == "ParticleEffect" || arrayType == "ParticleEffectDescription" || arrayType == "Sprite" || arrayType == "SpriteWrapper")
			{
				imports.add("import com.lyeeedar.Util.AssetManager")
			}
			else if (classRegister.enumMap.containsKey(arrayType))
			{
				imports.add("import java.util.*")
			}
		}
    }

    fun writeLoad(builder: IndentedStringBuilder, indentation: Int, classDefinition: ClassDefinition, classRegister: ClassRegister)
    {
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
                builder.appendln(indentation, "$name = xmlData.get(\"$dataName\")")
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = xmlData.get(\"$dataName\", $defaultValue)"
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
                builder.appendln(indentation, "$name = xmlData.getInt(\"$dataName\", $defaultValue)")
            }
        }
        else if (type == "Float")
        {
            if (variableType == VariableType.VAR)
            {
                builder.appendln(indentation, "$name = xmlData.getFloat(\"$dataName\", $defaultValue)")
            }
        }
		else if (type == "Boolean")
		{
			if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "$name = xmlData.getBoolean(\"$dataName\", $defaultValue)")
			}
		}
		else if (type == "Point" || type == "Vector2" || type == "Vector3" || type == "Vector4")
		{
			if (variableType == VariableType.VAR || variableType == VariableType.LATEINIT)
			{
				if (variableType == VariableType.LATEINIT)
				{
					builder.appendln(indentation, """val ${name}Raw = xmlData.get("$dataName").split(',')""")
				}
				else
				{
					var defaultValue = defaultValue.split('(')[1].dropLast(1)
					if (defaultValue.isBlank())
					{
						if (type == "Vector4")
						{
							defaultValue = "0,0,0,0"
						}
						else if (type == "Vector3")
						{
							defaultValue = "0,0,0"
						}
						else
						{
							defaultValue = "0,0"
						}
					}
					builder.appendln(indentation, """val ${name}Raw = xmlData.get("$dataName", "$defaultValue")!!.split(',')""")
				}

				if (type == "Point")
				{
					builder.appendln(indentation, "$name = Point(${name}Raw[0].toInt(), ${name}Raw[1].toInt())")
				}
				else if (type == "Vector2")
				{
					builder.appendln(indentation, "$name = Vector2(${name}Raw[0].toFloat(), ${name}Raw[1].toFloat())")
				}
				else if (type == "Vector3")
				{
					builder.appendln(indentation, "$name = Vector3(${name}Raw[0].toFloat(), ${name}Raw[1].toFloat(), ${name}Raw[2].toFloat())")
				}
				else
				{
					builder.appendln(indentation, "$name = Vector3(${name}Raw[0].toFloat(), ${name}Raw[1].toFloat(), ${name}Raw[2].toFloat(), ${name}Raw[3].toFloat())")
				}
			}
		}
        else if (type == "ParticleEffect" || type == "ParticleEffectDescription" || type == "Sprite" || type == "SpriteWrapper")
        {
			val loadName: String
			if (type == "ParticleEffectDescription" || type == "ParticleEffect")
			{
				loadName = "ParticleEffect"
			}
			else if (type == "Sprite")
			{
				loadName = "Sprite"
			}
			else if (type == "SpriteWrapper")
			{
				loadName = "SpriteWrapper"
			}
			else
			{
				throw RuntimeException("Unhandled renderable load type '$type'")
			}

			val loadExtension: String
			if (type == "ParticleEffect")
			{
				if (variableType == VariableType.LATEINIT)
				{
					loadExtension = ".getParticleEffect()"
				}
				else if (nullable)
				{
					loadExtension = "?.getParticleEffect()"
				}
				else
				{
					loadExtension = ""
				}
			}
			else
			{
				loadExtension = ""
			}

            if (variableType == VariableType.LATEINIT)
            {
                val loadLine = "$name = AssetManager.load$loadName(xmlData.getChildByName(\"$dataName\")!!)"
                builder.appendln(indentation, "$loadLine$loadExtension")
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = AssetManager.tryLoad$loadName(xmlData.getChildByName(\"$dataName\"))"
                if (!nullable)
                {
                    loadLine += "!!"
                }
                builder.appendln(indentation, "$loadLine$loadExtension")
            }
        }
		else if (classRegister.enumMap.containsKey(type))
		{
			val enumDef = classRegister.enumMap[type]!!

			if (variableType == VariableType.LATEINIT)
			{
				builder.appendln(indentation, "$name = ${enumDef.name}.valueOf(xmlData.get(\"$dataName\").toUpperCase(Locale.ENGLISH))")
			}
			else if (variableType == VariableType.VAR)
			{
				builder.appendln(indentation, "$name = ${enumDef.name}.valueOf(xmlData.get(\"$dataName\", ${defaultValue}.toString())!!.toUpperCase(Locale.ENGLISH))")
			}
		}
		else if (type.startsWith("Array<"))
		{
			val arrayType = type.replace("Array<", "").dropLast(1)

			val elName = name+"El"

			builder.appendln(indentation, "val $elName = xmlData.getChildByName(\"$dataName\")!!")
			builder.appendln(indentation, "for (el in ${elName}.children)")
			builder.appendln(indentation, "{")

			if (arrayType == "String")
			{
				builder.appendln(indentation+1, "$name.add(el.text)")
			}
			else if (arrayType == "Int")
			{
				builder.appendln(indentation+1, "$name.add(el.int())")
			}
			else if (arrayType == "Float")
			{
				builder.appendln(indentation+1, "$name.add(el.float())")
			}
			else if (arrayType == "Boolean")
			{
				builder.appendln(indentation+1, "$name.add(el.boolean())")
			}
			else if (arrayType == "Sprite" || arrayType == "SpriteWrapper" || arrayType == "ParticleEffectDescription" || arrayType == "ParticleEffect")
			{
				val loadName: String
				if (arrayType == "ParticleEffectDescription" || arrayType == "ParticleEffect")
				{
					loadName = "ParticleEffect"
				}
				else if (arrayType == "Sprite")
				{
					loadName = "Sprite"
				}
				else if (arrayType == "SpriteWrapper")
				{
					loadName = "SpriteWrapper"
				}
				else
				{
					throw RuntimeException("Unhandled renderable load type '$arrayType'")
				}

				val loadExtension: String
				if (arrayType == "ParticleEffect")
				{
					loadExtension = ".getParticleEffect()"
				}
				else
				{
					loadExtension = ""
				}

				builder.appendln(indentation+1, "val obj = AssetManager.load$loadName(el)$loadExtension")
				builder.appendln(indentation+1, "$name.add(obj)")
			}
			else if (classRegister.enumMap.containsKey(arrayType))
			{
				val enumDef = classRegister.enumMap[arrayType]!!
				builder.appendln(indentation+1, "val obj = ${enumDef.name}.valueOf(el.text.toUpperCase(Locale.ENGLISH))")
				builder.appendln(indentation+1, "$name.add(obj)")
			}
			else
			{
				val classDef = classRegister.classMap[arrayType] ?: throw RuntimeException("writeLoad: Unknown type '$arrayType' in '$type'!")
				classDefinition.referencedClasses.add(classDef)

				if (classDef.isAbstract)
				{
					builder.appendln(indentation+1, "val obj = $arrayType.loadPolymorphicClass(el.get(\"classID\"))")
				}
				else
				{
					builder.appendln(indentation+1, "val obj = $arrayType()")
				}

				builder.appendln(indentation+1, "obj.load(el)")
				builder.appendln(indentation+1, "$name.add(obj)")
			}

			builder.appendln(indentation, "}")
		}
        else
        {
            val classDef = classRegister.classMap[type] ?: throw RuntimeException("writeLoad: Unknown type '$type'!")

            classDefinition.referencedClasses.add(classDef)

            val el = name+"El"
            if (variableType == VariableType.LATEINIT || (variableType == VariableType.VAR && !nullable))
            {
                builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$dataName\")!!")

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
                builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$dataName\")")
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
                builder.appendln(indentation, "val $el = xmlData.getChildByName(\"$dataName\")!!")
                builder.appendln(indentation, "$name.load($el)")
            }
        }
    }

    fun createDefEntry(builder: IndentedStringBuilder, classRegister: ClassRegister)
    {
        if (variableType == VariableType.VAL && name == "classID")
        {
			val defaultValue = defaultValue.replace("\"", "")
            builder.appendln(2, """<Const Name="classID">$defaultValue</Const>""")
            return
        }

        var type = type
        var isNullable = false
        if (type.endsWith('?'))
        {
            type = type.substring(0, type.length-1)
            isNullable = true
        }

        var nullable = ""
        var skipIfDefault = ""

        if (variableType == VariableType.LATEINIT)
        {
            nullable = """Nullable="False""""
            skipIfDefault = """SkipIfDefault="False""""
        }
        else if (variableType == VariableType.VAL)
        {
            nullable = """Nullable="False""""
            skipIfDefault = """SkipIfDefault="False""""
        }
        else
        {
            if (isNullable)
            {
                nullable = """Nullable="True""""
                skipIfDefault = """SkipIfDefault="True""""
            }
            else
            {
                nullable = """Nullable="False""""
                skipIfDefault = """SkipIfDefault="False""""
            }
        }

        if (type == "String")
        {
			val canSkip = if (variableType != VariableType.LATEINIT) "True" else "False"
			val defaultValue = if (this.defaultValue.isBlank() || this.defaultValue == "null") "\"\"" else this.defaultValue

			val fileReferenceAnnotation = annotations.firstOrNull { it.name == "FileReference" }
			if (fileReferenceAnnotation != null)
			{
				val basePath = fileReferenceAnnotation.paramMap["basePath"]
				val stripExtension = fileReferenceAnnotation.paramMap["stripExtension"]
				val resourceType = fileReferenceAnnotation.paramMap["resourceType"]
				val allowedFileTypes = fileReferenceAnnotation.paramMap["allowedFileTypes"]

				val basePathStr = if (basePath != null) "BasePath=\"$basePath\"" else ""
				val stripExtensionStr = if (stripExtension != null) "StripExtension=\"$stripExtension\"" else "StripExtension=\"True\""
				val resourceTypeStr = if (resourceType != null) "ResourceType=\"$resourceType\"" else ""
				val allowedFileTypesStr = if (allowedFileTypes != null) "AllowedFileTypes=\"$allowedFileTypes\"" else ""

				builder.appendlnFix(2, """<Data Name="$dataName" $basePathStr $stripExtensionStr $resourceTypeStr $allowedFileTypesStr SkipIfDefault="$canSkip" Default=$defaultValue $visibleIfStr meta:RefKey="File" />""")
			}
			else
			{
				val localisationAnnotation = annotations.firstOrNull { it.name == "NeedsLocalisation" }
				val needsLocalisation = if (localisationAnnotation != null) "NeedsLocalisation=\"True\"" else ""
				val localisationFile = if (localisationAnnotation != null) "LocalisationFile=\"${localisationAnnotation.paramMap["file"]!!}\"" else ""

				builder.appendlnFix(2, """<Data Name="$dataName" $needsLocalisation $localisationFile SkipIfDefault="$canSkip" Default=$defaultValue $visibleIfStr meta:RefKey="String" />""")
			}
        }
        else if (type == "Int" || type == "Float")
        {
            val numericAnnotation = annotations.firstOrNull { it.name == "NumericRange" }
            val min = numericAnnotation?.paramMap?.get("min")?.replace("f", "")
            val max = numericAnnotation?.paramMap?.get("max")?.replace("f", "")
            val minStr = if (min != null) """Min="$min"""" else ""
            val maxStr = if (max != null) """Max="$max"""" else ""
			val defaultValue = this.defaultValue.replace("f", "")

            builder.appendlnFix(2, """<Data Name="$dataName" $minStr $maxStr Type="$type" Default="$defaultValue" SkipIfDefault="True" $visibleIfStr meta:RefKey="Number" />""")
        }
		else if (type == "Point" || type == "Vector2" || type == "Vector3" || type == "Vector4")
		{
			val numericAnnotation = annotations.firstOrNull { it.name == "NumericRange" }
			val min = numericAnnotation?.paramMap?.get("min")?.replace("f", "")
			val max = numericAnnotation?.paramMap?.get("max")?.replace("f", "")
			val minStr = if (min != null) """Min="$min"""" else ""
			val maxStr = if (max != null) """Max="$max"""" else ""
			val defaultValue = this.defaultValue.split('(')[1].dropLast(1).replace("f", "")
			val numericType = if (type == "Point") "Type=\"Int\"" else ""

			val vectorAnnotation = annotations.firstOrNull { it.name == "Vector" }
			val name1 = vectorAnnotation?.paramMap?.get("name1")
			val name2 = vectorAnnotation?.paramMap?.get("name2")
			val name3 = vectorAnnotation?.paramMap?.get("name3")
			val name4 = vectorAnnotation?.paramMap?.get("name4")
			val name1Str = if (name1 != null) "Name1=\"$name1\"" else ""
			val name2Str = if (name2 != null) "Name2=\"$name2\"" else ""
			val name3Str = if (name3 != null) "Name3=\"$name3\"" else ""
			val name4Str = if (name4 != null) "Name4=\"$name4\"" else ""
			val numComponents = if (type == "Point") "NumComponents=\"2\"" else "NumComponents=\"${type.last()}\""

			builder.appendlnFix(2, """<Data Name="$dataName" $minStr $maxStr $numericType $name1Str $name2Str $name3Str $name4Str $numComponents SkipIfDefault="True" Default="$defaultValue" $visibleIfStr meta:RefKey="Vector" />""")
		}
		else if (type == "Boolean")
		{
			builder.appendlnFix(2, """<Data Name="$dataName" SkipIfDefault="True" Default="$defaultValue" $visibleIfStr meta:RefKey="Boolean" />""")
		}
        else if (type == "Sprite" || type == "SpriteWrapper" || type == "ParticleEffect" || type == "ParticleEffectDescription")
        {
			val dataType = if (type == "ParticleEffectDescription") "ParticleEffect" else type
            builder.appendlnFix(2, """<Data Name="$dataName" Keys="$dataType" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
        }
		else if (classRegister.enumMap.containsKey(type))
		{
			val enumDef = classRegister.enumMap[type]!!
            val enumVals = enumDef.values.joinToString(",")

            var defaultStr = ""
            if (defaultValue.isNotBlank())
            {
                defaultStr = "Default=\"${defaultValue.split('.').last()}\""
            }

            builder.appendlnFix(2, """<Data Name="$dataName" EnumValues="$enumVals" $defaultStr $skipIfDefault $visibleIfStr meta:RefKey="Enum" />""")
		}
		else if (type.startsWith("Array<"))
		{
			val arrayType = type.replace("Array<", "").dropLast(1)

			val dataArrayAnnotation = annotations.firstOrNull { it.name == "DataArray" }
			val minCount = dataArrayAnnotation?.paramMap?.get("minCount")
			val maxCount = dataArrayAnnotation?.paramMap?.get("maxCount")
			val minCountStr = if (minCount != null) "MinCount=\"$minCount\"" else ""
			val maxCountStr = if (maxCount != null) "MaxCount=\"$maxCount\"" else ""

			val childName = if (dataName.endsWith('s')) dataName.dropLast(1) else arrayType
			if (arrayType == "String")
			{
				val fileReferenceAnnotation = annotations.firstOrNull { it.name == "FileReference" }
				if (fileReferenceAnnotation != null)
				{
					val basePath = fileReferenceAnnotation.paramMap["basePath"]
					val stripExtension = fileReferenceAnnotation.paramMap["stripExtension"]
					val resourceType = fileReferenceAnnotation.paramMap["resourceType"]
					val allowedFileTypes = fileReferenceAnnotation.paramMap["allowedFileTypes"]

					val basePathStr = if (basePath != null) "BasePath=\"$basePath\"" else ""
					val stripExtensionStr = if (stripExtension != null) "StripExtension=\"$stripExtension\"" else "StripExtension=\"True\""
					val resourceTypeStr = if (resourceType != null) "ResourceType=\"$resourceType\"" else ""
					val allowedFileTypesStr = if (allowedFileTypes != null) "AllowedFileTypes=\"$allowedFileTypes\"" else ""

					builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
					builder.appendlnFix(3, """<Data Name="$childName" $basePathStr $stripExtensionStr $resourceTypeStr $allowedFileTypesStr meta:RefKey="File"/>""")
					builder.appendlnFix(2, """</Data>""")
				}
				else
				{
					val localisationAnnotation = annotations.firstOrNull { it.name == "NeedsLocalisation" }
					val needsLocalisation = if (localisationAnnotation != null) "NeedsLocalisation=\"True\"" else ""
					val localisationFile = if (localisationAnnotation != null) "LocalisationFile=\"${localisationAnnotation.paramMap["localisationFile"]!!}\"" else ""

					builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
					builder.appendlnFix(3, """<Data Name="$childName" $needsLocalisation $localisationFile meta:RefKey="String"/>""")
					builder.appendlnFix(2, """</Data>""")
				}
			}
			else if (arrayType == "Int" || arrayType == "Float")
			{
				val numericAnnotation = annotations.firstOrNull { it.name == "NumericRange" }
				val min = numericAnnotation?.paramMap?.get("min")?.replace("f", "")
				val max = numericAnnotation?.paramMap?.get("max")?.replace("f", "")
				val minStr = if (min != null) """Min="$min"""" else ""
				val maxStr = if (max != null) """Max="$max"""" else ""

				builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
				builder.appendlnFix(3, """<Data Name="$childName" $minStr $maxStr Type="$arrayType" SkipIfDefault="True" meta:RefKey="Number" />""")
				builder.appendlnFix(2, """</Data>""")
			}
			else if (arrayType == "Boolean")
			{
				builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
				builder.appendlnFix(3, """<Data Name="$childName" meta:RefKey="Boolean">""")
				builder.appendlnFix(2, """</Data>""")
			}
			else if (arrayType == "Sprite" || arrayType == "SpriteWrapper" || arrayType == "ParticleEffect" || arrayType == "ParticleEffectDescription")
			{
				val dataType = if (arrayType == "ParticleEffectDescription") "ParticleEffect" else arrayType
				builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
				builder.appendlnFix(3, """<Data Name="$childName" Keys="$dataType" Nullable="False" meta:RefKey="Reference" />""")
				builder.appendlnFix(2, """</Data>""")
			}
			else if (classRegister.enumMap.containsKey(arrayType))
			{
				val enumDef = classRegister.enumMap[arrayType]!!
				val enumVals = enumDef.values.joinToString(",")

				builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr $visibleIfStr meta:RefKey="Collection">""")
				builder.appendlnFix(3, """<Data Name="$childName" EnumValues="$enumVals" meta:RefKey="Enum" />""")
				builder.appendlnFix(2, """</Data>""")
			}
			else
			{
				val classDef = classRegister.classMap[arrayType] ?: throw RuntimeException("createDefEntry: Unknown type '$arrayType' for '$type'!")

				if (classDef.isAbstract)
				{
					builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr DefKey="${classDef.classDef!!.dataClassName}Defs" $visibleIfStr meta:RefKey="Collection" />""")
				}
				else
				{
					builder.appendlnFix(2, """<Data Name="$dataName" $minCountStr $maxCountStr Keys="${classDef.classDef!!.dataClassName}" $visibleIfStr meta:RefKey="Collection" />""")
				}
			}
		}
        else
        {
            val classDef = classRegister.classMap[type] ?: throw RuntimeException("createDefEntry: Unknown type '$type'!")

            if (classDef.isAbstract)
            {
                builder.appendlnFix(2, """<Data Name="$dataName" DefKey="${classDef.classDef!!.dataClassName}Defs" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
            }
            else
            {
                builder.appendlnFix(2, """<Data Name="$dataName" Keys="${classDef.classDef!!.dataClassName}" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
            }
        }
    }
}