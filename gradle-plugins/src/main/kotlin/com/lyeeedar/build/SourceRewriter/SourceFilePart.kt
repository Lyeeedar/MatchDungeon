package com.lyeeedar.build.SourceRewriter

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
		for (import in imports.sorted())
		{
			builder.appendln(import.trimEnd())
		}
    }
}

class MiscFilePart : IFilePart
{
    val code = ArrayList<String>()

    override fun write(builder: IndentedStringBuilder)
    {
		for (line in code)
		{
			builder.appendln(line.trimEnd())
		}
    }
}

class DataClassFilePart(name: String, superClass: String, classIndentation: Int, classDefinition: ClassDefinition, classRegister: ClassRegister, annotations: ArrayList<AnnotationDescription>) : IFilePart
{
    val desc: XmlDataClassDescription = XmlDataClassDescription(name, superClass, classIndentation, classDefinition, classRegister, annotations)

    override fun write(builder: IndentedStringBuilder)
    {
        desc.write(builder)
    }
}