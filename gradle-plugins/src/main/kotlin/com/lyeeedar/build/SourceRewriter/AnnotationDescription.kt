package com.lyeeedar.build.SourceRewriter

class AnnotationDescription(val annotationString: String)
{
    val name: String = annotationString.split("(")[0].substring(1)
    val paramMap = HashMap<String, String>()
        get()
        {
            if (!isParsed)
            {
				isParsed = true
                parse()
            }

            return field
        }
    private var isParsed = false

    fun parse()
    {
        val parameters = annotationString.replace("@$name(", "").replace(")", "")

		if (!parameters.isBlank())
		{
			val split = parameters.split(",")

			for (parameter in split)
			{
				if (!parameter.contains("="))
				{
					System.err.println("Non-named annotation parameters are not currently supported! ($annotationString) ($parameter)")
					continue
				}

				val split = parameter.split("=")
				val paramName = split[0].trim()
				val paramValue = parameter.replace("${split[0]}=", "").trim().replace("\"", "")

				paramMap.put(paramName, paramValue)
			}
		}
    }
}