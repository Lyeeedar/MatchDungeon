package com.lyeeedar.headless

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.headless.HeadlessApplication
import com.badlogic.gdx.graphics.GL20
import com.lyeeedar.ResourceProcessors.AtlasCreator
import com.lyeeedar.ResourceProcessors.TextureCompressor
import com.lyeeedar.ResourceProcessors.XmlCompressor
import com.lyeeedar.ResourceProcessors.XmlLoadTester
import com.lyeeedar.ResourceProcessors.OryxExtractor
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

			OryxExtractor()
			AtlasCreator()
			TextureCompressor()
			XmlCompressor()
			XmlLoadTester.test()
		}
		catch (ex: Exception)
		{
			println("Compiling failed!")
			System.err.println(ex.toString())
			throw RuntimeException("Compiling failed")
		}
		finally {
			Gdx.app.exit()
			println("##########################################################")
		}
	}
}
