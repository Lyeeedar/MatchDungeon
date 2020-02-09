package com.lyeeedar.build

import com.lyeeedar.build.SourceRewriter.ClassRegister
import com.lyeeedar.build.SourceRewriter.SourceRewriter
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction
import java.io.File

class SourceRewriterPlugin : Plugin<Project>
{
	override fun apply(target: Project)
	{
		target.tasks.create("rewriteSources", SourceRewriterTask::class.java)
	}
}

open class SourceRewriterTask : DefaultTask()
{
	@InputFiles
	var inputDirs: LinkedHashSet<File>? = null

	@OutputDirectories
	var srcDirs: LinkedHashSet<File>? = null

	@TaskAction
	fun rewriteSources()
	{
		try
		{
			println("################################################")
			println("Source Rewrite starting")
			println("")

			println("Parsing source files")
			val srcFiles = find(srcDirs!!)

			val defsDir = File(srcDirs!!.first().absolutePath + "/../../../android/assetsraw/Definitions/Generated").canonicalFile

			val classRegister = ClassRegister(srcFiles, defsDir)
			classRegister.registerClasses()

			val dataClassFiles = ArrayList<SourceRewriter>()
			for (file in srcFiles)
			{
				val rewriter = SourceRewriter(file, classRegister)
				val hasDataClass = rewriter.parse()
				if (hasDataClass)
				{
					dataClassFiles.add(rewriter)
				}
			}

			println("Writing changes")
			for (rewriter in dataClassFiles)
			{
				rewriter.write()
			}

			println("Writing def files")
			classRegister.writeXmlDefFiles()

			println("")
			println("Source Rewrite completed")
			println("################################################")
		}
		catch (ex: Exception)
		{
			ex.printStackTrace()
			throw ex
		}
	}

	fun find(roots: LinkedHashSet<File>): List<File>
	{
		val files: MutableList<File> = ArrayList()
		for (root in roots)
		{
			if (!root.isDirectory) throw IllegalAccessError("$root must be a folder.")
			addFiles(files, root)
		}
		return files
	}

	private fun addFiles(files: MutableList<File>, folder: File)
	{
		for (f in folder.listFiles())
		{
			if (f.isFile && f.name.endsWith(".kt"))
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