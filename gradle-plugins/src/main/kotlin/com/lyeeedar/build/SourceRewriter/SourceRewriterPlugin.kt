package com.lyeeedar.build

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



		println("Source Rewrite completed")
		println("################################################")
	}
}