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
			var parameters = parameters
			while (true)
			{
				val nextSplit = nextNonQuotedComma(parameters)
				val parameter = if (nextSplit != -1) parameters.substring(0, nextSplit) else parameters

				if (!parameter.contains("="))
				{
					System.err.println("Non-named annotation parameters are not currently supported! ($annotationString) ($parameter)")
					continue
				}

				val split = parameter.split("=")
				val paramName = split[0].trim()
				val paramValue = parameter.replace("${split[0]}=", "").trim().replace("\"", "")

				paramMap.put(paramName, paramValue)

				if (nextSplit == -1)
				{
					break
				}
				else
				{
					parameters = parameters.substring(nextSplit+1)
				}
			}
		}
    }

	fun nextNonQuotedComma(str: String): Int
	{
		var inQuotes = false
		for (i in str.indices)
		{
			if (str[i] == '"')
			{
				inQuotes = !inQuotes
			}
			else if (!inQuotes && str[i] == ',')
			{
				return i
			}
		}

		return -1
	}
}