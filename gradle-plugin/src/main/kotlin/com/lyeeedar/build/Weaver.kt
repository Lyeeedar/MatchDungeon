package com.lyeeedar.build

import org.objectweb.asm.ClassReader
import org.objectweb.asm.Type
import java.io.File

class Weaver(val classesDirs: Set<File>)
{
	fun execute()
	{
		val classes: List<File> = find(classesDirs)

		for (classFile in classes)
		{
			processFile(classFile)
		}
	}

	fun processFile(file: File)
	{
		println("processing file ${file.absolutePath}")

		val cr = ClassReader(file.inputStream())
		val meta = ClassMetadata()
		meta.type = Type.getObjectType(cr.className)
		cr.accept(MetaScanner(meta), 0)
	}

	fun find(roots: Set<File>): List<File>
	{
		val klazzes: MutableList<File> = ArrayList()
		for (root in roots)
		{
			if (!root.isDirectory) throw IllegalAccessError("$root must be a folder.")
			addFiles(klazzes, root)
		}
		return klazzes
	}

	private fun addFiles(files: MutableList<File>, folder: File)
	{
		for (f in folder.listFiles())
		{
			if (f.isFile && f.name.endsWith(".class"))
			{
				files.add(f)
			}
			else if (f.isDirectory)
			{
				addFiles(files, f)
			}
		}
	}
}