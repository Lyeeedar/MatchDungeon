package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.lyeeedar.Game.Global
import com.lyeeedar.Game.GlobalDeck
import com.lyeeedar.Util.Statics
import org.mockito.Mockito

object LongTest
{
	@JvmStatic fun main(arg: Array<String>)
	{
		println("##########################################################")
		try
		{
			Statics.test = true
			Gdx.gl = Mockito.mock(GL20::class.java)
			Gdx.gl20 = Mockito.mock(GL20::class.java)
			Gdx.app = HeadlessApplication(Mockito.mock(Game::class.java))
			Global.deck = GlobalDeck()

			val start = System.currentTimeMillis()

			val funcs = com.badlogic.gdx.utils.Array<Pair<String, ()->Any>>()
			funcs.add(Pair("ReplayTest", { LevelSolver().attemptAllLevels(true) }))
			funcs.add(Pair("CheckSolvability", { LevelSolver().checkSolvability() }))
			funcs.add(Pair("DetermineDifficulty", { LevelSolver().determineDifficulty() }))

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

			println("Testing complete in $seconds seconds")

			for (timing in timings)
			{
				println("Completed ${timing.first} in ${timing.second/1000} seconds")
			}
		}
		catch (ex: Exception)
		{
			println("Testing failed!")
			System.err.println(ex.toString())
			throw ex
		}
		finally {
			Gdx.app.exit()
			println("##########################################################")
		}
	}
}