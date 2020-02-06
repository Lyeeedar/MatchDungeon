package com.lyeeedar.build.SourceRewriter

class XmlDataClassDescription(val classDefinition: ClassDefinition, val classRegister: ClassRegister, val annotations: ArrayList<AnnotationDescription>)
{
    lateinit var name: String
    lateinit var superClass: String
    var classIndentation = 0
    val variables = ArrayList<VariableDescription>()

    init
    {
        classDefinition.classDef = this
    }

    fun resolveImports(imports: HashSet<String>)
    {
        for (variable in variables)
        {
            variable.resolveImports(imports, classDefinition, classRegister)
        }

        if (classDefinition.isAbstract)
        {
            for (childClass in classDefinition.inheritingClasses)
            {
                if (!childClass.isAbstract)
                {
                    if (childClass.packageStr != classDefinition.packageStr)
                    {
                        imports.add(childClass.packageStr.replace("package ", "").trim() + ".${childClass.name}")
                    }
                }
            }
        }
    }

    fun write(builder: IndentedStringBuilder)
    {
        for (annotation in annotations)
        {
            builder.appendln(classIndentation, annotation.annotationString)
        }

        val classType = if (classDefinition.isAbstract) "abstract class" else "class"
        builder.appendln(classIndentation, "$classType $name : $superClass")
        builder.appendln(classIndentation, "{")

        for (variable in variables)
        {
            for (annotation in variable.annotations)
            {
                builder.appendln(classIndentation+1, annotation.annotationString)
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
            variable.writeLoad(builder, classIndentation+2, classDefinition, classRegister)
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

    fun createDefFile(builder: IndentedStringBuilder, needsGlobalScope: Boolean)
    {
        val dataFileAnnotation = annotations.firstOrNull { it.name == "XmlDataFile" }
        if (dataFileAnnotation != null)
        {
            val name = dataFileAnnotation.paramMap["name"] ?: name
            builder.appendln(1, "<Definition Name=\"$name\" meta:RefKey=\"Struct\"")
        }
        else
        {
            val global = if (needsGlobalScope) "IsGlobal=\"True\"" else ""
            builder.appendln(1, "<Definition Name=\"$name\" $global meta:RefKey=\"StructDef\"")
        }

        for (variable in variables)
        {
            variable.createDefEntry(builder)
        }

        builder.appendln(1, "</Definition>")
    }
}