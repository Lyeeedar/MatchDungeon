package com.lyeeedar.build.SourceRewriter

enum class VariableType
{
    VAR,
    VAL,
    LATEINIT
}

class VariableDescription(val variableType: VariableType, val name: String, val type: String, val defaultValue: String, val raw: String, val annotations: ArrayList<AnnotationDescription>)
{
    fun resolveImports(imports: HashSet<String>, classDefinition: ClassDefinition, classRegister: ClassRegister)
    {
        if (type == "ParticleEffect" || type == "ParticleEffectDescription" || type == "Sprite")
        {
            imports.add("import com.lyeeedar.Util.AssetManager")
        }
        else if (type == "String" || type == "Int" || type == "Float")
        {
            // primitives dont need imports
        }
        else
        {
            val classDef = classRegister.classMap[type] ?: throw RuntimeException("Unknown type '$type'!")

            if (classDef.packageStr != classDefinition.packageStr)
            {
                imports.add(classDef.packageStr.replace("package ", "").trim() + ".${classDef.name}")
            }
        }
    }

    fun writeLoad(builder: IndentedStringBuilder, indentation: Int, classRegister: ClassRegister)
    {
        var dataName = name.capitalize()
        for (annotation in annotations)
        {
            if (annotation.name == "XmlDataValue")
            {
                val customDataName = annotation.paramMap["dataName"]
                if (customDataName != null && customDataName != "")
                {
                    dataName = customDataName
                }
            }
        }

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
        else
        {
            val classDef = classRegister.classMap[type] ?: throw RuntimeException("Unknown type '$type'!")

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
}