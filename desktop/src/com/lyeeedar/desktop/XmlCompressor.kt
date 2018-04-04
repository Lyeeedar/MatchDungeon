package com.lyeeedar.desktop

import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getRawXml
import java.io.File

class XmlCompressor
{
	val rootPath: String

	init
	{
		rootPath = File("").absolutePath

		// clear out existing data
		val outputFolder = File("CompressedData")
		for (file in outputFolder.list())
		{
			if (file.endsWith(".xmldata"))
			{
				val deleted = File("CompressedData/" + file).delete()
				if (!deleted)
				{
					error("Failed to delete file $file!")
				}
			}
		}

		findFilesRecursive(File("").absoluteFile, true)
	}

	private fun findFilesRecursive(dir: File, isRoot: Boolean)
	{
		val contents = dir.listFiles() ?: return

		for (file in contents)
		{
			if (file.isDirectory)
			{
				findFilesRecursive(file, false)
			}
			else if (!isRoot && file.path.endsWith(".xml"))
			{
				processXml(file.path)
			}
		}
	}

	private fun processXml(path: String)
	{
		val relativePath = path.replace(rootPath + "\\", "").replace("\\", "/")

		val rawxml = getRawXml(relativePath)
		val data = XmlData.loadFromElement(rawxml)

		val outputPath = "CompressedData/" + relativePath.hashCode() + ".xmldata"

		data.save(outputPath)

		// try to load it
		XmlData().load(outputPath)

		System.out.println("Compressed $relativePath")
	}
}