package com.lyeeedar.build

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.TaskAction

//gradlew :core:build  --stacktrace -Dorg.gradle.daemon=false   -Dorg.gradle.debug=true
class WeaverGradlePlugin : Plugin<Project>
{
	override fun apply(target: Project)
	{
		target.tasks.create("weave", WeavingTask::class.java)
	}
}

open class WeavingTask : DefaultTask()
{
	@OutputDirectories
	var classesDirs: FileCollection? = null

	@TaskAction fun weave()
	{
		println("################################################")
		println("Weave starting")
		println("")

		val weaver = Weaver(classesDirs!!.files)
		weaver.execute()

		println("Weave completed")
		println("################################################")
	}
}