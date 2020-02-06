package com.lyeeedar.build.SourceRewriter

class AnnotationDescription(val annotationString: String)
{
    val name: String = annotationString.split("(")[0].substring(1)
    val paramMap = HashMap<String, String>()
        get()
        {
            if (!isParsed)
            {
                parse()
                isParsed = true
            }

            return field
        }
    private var isParsed = false

    fun parse()
    {
        val parameters = annotationString.replace("@$name(", "").replace(")", "")
        val split = parameters.split(",")

        for (parameter in split)
        {
            if (!split.contains("="))
			{
				System.err.println("Non-named annotation parameters are not currently supported!")
				continue
			}

            val split = parameter.split("=")
            val paramName = split[0].trim()
            val paramValue = split[1].trim()

            paramMap.put(paramName, paramValue)
        }
    }
}