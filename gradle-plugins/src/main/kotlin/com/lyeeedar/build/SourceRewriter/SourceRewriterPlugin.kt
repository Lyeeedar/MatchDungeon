package com.lyeeedar.build

import com.lyeeedar.build.SourceRewriter.SourceRewriter
import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
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
	@OutputDirectories
	var srcDirs: LinkedHashSet<File>? = null

	@TaskAction
	fun rewriteSources()
	{
		println("################################################")
		println("Source Rewrite starting")
		println("")

		for (file in find(srcDirs!!))
		{
			SourceRewriter(file).rewrite()
		}

		println("")
		println("Source Rewrite completed")
		println("################################################")
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