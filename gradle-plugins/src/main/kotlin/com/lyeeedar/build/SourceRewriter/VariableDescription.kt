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
		else if (classRegister.enumMap.containsKey(type))
		{
			imports.add("import java.util.*")
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
        else if (type == "ParticleEffect")
        {
            if (variableType == VariableType.LATEINIT)
            {
                val loadLine = "$name = AssetManager.loadParticleEffect(xmlData.getChildByName(\"$dataName\")!!)"
                builder.appendln(indentation, "${loadLine}.getParticleEffect()")
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = AssetManager.tryLoadParticleEffect(xmlData.getChildByName(\"$dataName\"))"
                if (!nullable)
                {
                    loadLine += "!!"
                }
				else
				{
					loadLine += "?"
				}
                builder.appendln(indentation, "${loadLine}.getParticleEffect()")
            }
        }
        else if (type == "ParticleEffectDescription")
        {
            if (variableType == VariableType.LATEINIT)
            {
                val loadLine = "$name = AssetManager.loadParticleEffect(xmlData.getChildByName(\"$dataName\")!!)"
                builder.appendln(indentation, loadLine)
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = AssetManager.tryLoadParticleEffect(xmlData.getChildByName(\"$dataName\"))"
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
                val loadLine = "$name = AssetManager.loadSprite(xmlData.getChildByName(\"$dataName\")!!)"
                builder.appendln(indentation, loadLine)
            }
            else if (variableType == VariableType.VAR)
            {
                var loadLine = "$name = AssetManager.tryLoadSprite(xmlData.getChildByName(\"$dataName\"))"
                if (!nullable)
                {
                    loadLine += "!!"
                }
                builder.appendln(indentation, loadLine)
            }
        }
		else if (type == "SpriteWrapper")
		{
			if (variableType == VariableType.LATEINIT)
			{
				val loadLine = "$name = AssetManager.loadSpriteWrapper(xmlData.getChildByName(\"$dataName\")!!)"
				builder.appendln(indentation, loadLine)
			}
			else if (variableType == VariableType.VAR)
			{
				var loadLine = "$name = AssetManager.tryLoadSpriteWrapper(xmlData.getChildByName(\"$dataName\"))"
				if (!nullable)
				{
					loadLine += "!!"
				}
				builder.appendln(indentation, loadLine)
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

			val classDef = classRegister.classMap[arrayType] ?: throw RuntimeException("writeLoad: Unknown type '$arrayType' in '$type'!")
			classDefinition.referencedClasses.add(classDef)

			val elName = name+"El"

			builder.appendln(indentation, "val $elName = xmlData.getChildByName(\"$dataName\")!!")
			builder.appendln(indentation, "for (el in ${elName}.children)")
			builder.appendln(indentation, "{")

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
			val defaultValue = if (this.defaultValue.isBlank()) "\"\"" else this.defaultValue
            builder.appendlnFix(2, """<Data Name="$dataName" SkipIfDefault="$canSkip" Default=$defaultValue $visibleIfStr meta:RefKey="String" />""")
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
		else if (type == "Boolean")
		{
			builder.appendlnFix(2, """<Data Name="$dataName" SkipIfDefault="True" Default="$defaultValue" $visibleIfStr meta:RefKey="Boolean" />""")
		}
        else if (type == "Sprite")
        {
            builder.appendlnFix(2, """<Data Name="$dataName" Keys="Sprite" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
        }
		else if (type == "SpriteWrapper")
		{
			builder.appendlnFix(2, """<Data Name="$dataName" Keys="SpriteWrapper" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
		}
        else if (type == "ParticleEffect" || type == "ParticleEffectDescription")
        {
            builder.appendlnFix(2, """<Data Name="$dataName" Keys="ParticleEffect" $nullable $skipIfDefault $visibleIfStr meta:RefKey="Reference" />""")
        }
		else if (classRegister.enumMap.containsKey(type))
		{
			val enumDef = classRegister.enumMap[type]!!
            val enumVals = enumDef.values.sorted().joinToString(",")

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

			val classDef = classRegister.classMap[arrayType] ?: throw RuntimeException("createDefEntry: Unknown type '$arrayType' for '$type'!")

			if (classDef.isAbstract)
			{
				builder.appendlnFix(2, """<Data Name="$dataName" DefKey="${classDef.classDef!!.dataClassName}Defs" $visibleIfStr meta:RefKey="Collection" />""")
			}
			else
			{
				builder.appendlnFix(2, """<Data Name="$dataName" Keys="${classDef.classDef!!.dataClassName}" $visibleIfStr meta:RefKey="Collection" />""")
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