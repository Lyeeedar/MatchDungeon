package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.lyeeedar.ResourceProcessors.*
import org.mockito.Mockito

object CompilerRunner
{
	@JvmStatic fun main(arg: Array<String>)
	{
		println("##########################################################")
		try
		{
			Gdx.gl = Mockito.mock(GL20::class.java)
			Gdx.gl20 = Mockito.mock(GL20::class.java)
			Gdx.app = HeadlessApplication(Mockito.mock(Game::class.java))

			val start = System.currentTimeMillis()

			val funcs = com.badlogic.gdx.utils.Array<Pair<String, ()->Any>>()
			funcs.add(Pair("Oryx", { OryxExtractor() }))
			funcs.add(Pair("Atlas", { AtlasCreator() }))
			funcs.add(Pair("Texture", { TextureCompressor() }))
			funcs.add(Pair("Xml", { XmlCompressor() }))
			funcs.add(Pair("Test", { XmlLoadTester.test() }))
			funcs.add(Pair("Solve", { LevelSolver().attemptAllLevels() }))

			val timings = com.badlogic.gdx.utils.Array<Pair<String, Long>>()
			for (func in funcs)
			{
				val start = System.currentTimeMillis()
				func.second.invoke()
				val end = System.currentTimeMillis()
				timings.add(Pair(func.first, end - start))
			}

			val end = System.currentTimeMillis()

			val diff = end - start
			val seconds = diff / 1000

			println("Compilation complete in $seconds seconds")

			for (timing in timings)
			{
				println("Completed ${timing.first} in ${timing.second/1000} seconds")
			}
		}
		catch (ex: Exception)
		{
			println("Compiling failed!")
			System.err.println(ex.toString())
			throw ex
		}
		finally {
			Gdx.app.exit()
			println("##########################################################")
		}
	}
}
