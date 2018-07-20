package com.lyeeedar.Screens

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.lyeeedar.Global
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.KeyMapping
import com.lyeeedar.Util.KeySource
import ktx.actors.setKeyboardFocus


/**
 * Created by Philip on 20-Mar-16.
 */

abstract class AbstractScreen() : Screen, InputProcessor
{
    //############################################################################
    //region Abstract Methods

    abstract fun create()
    abstract fun doRender(delta: Float)

    //endregion
    //############################################################################
    //region Screen

	// ----------------------------------------------------------------------
	fun fadeOutTransition(time: Float)
	{
		fadeTime = time
		fadeAccumulator = time
		fadeType = FadeType.OUT
	}

	// ----------------------------------------------------------------------
	fun fadeInTransition(time: Float)
	{
		fadeTime = time
		fadeAccumulator = time
		fadeType = FadeType.IN
	}

	// ----------------------------------------------------------------------
	fun swapTo()
	{
		Global.game.switchScreen(this)
	}

    // ----------------------------------------------------------------------
    override fun show()
	{
        if ( !created )
		{
            baseCreate()
            created = true
        }

        Gdx.input.inputProcessor = inputMultiplexer
    }

    // ----------------------------------------------------------------------
    override fun resize(width: Int, height: Int)
	{
        stage.viewport.update(width, height, true)
    }

    // ----------------------------------------------------------------------
    override fun render(delta: Float)
	{
		val start = System.nanoTime()

        stage.act()
		Future.update(delta)

        Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        doRender(delta)

        stage.draw()

		if (fadeAccumulator > 0f)
		{
			fadeAccumulator -= delta
			var alpha = fadeAccumulator / fadeTime

			if (fadeType == FadeType.IN)
			{
				alpha = 1f - alpha
			}

			fadeRenderer.begin(ShapeRenderer.ShapeType.Filled)
			fadeRenderer.setColor(0f, 0f, 0f, alpha)
			fadeRenderer.rect(-1f, -1f, 2f, 2f) //full screen rect w/ identity matrix
			fadeRenderer.end()
		}

		val end = System.nanoTime()

		val diff = (end - start) / 1000000000f
		frameDuration = (frameDuration + diff) / 2f

		fpsAccumulator += delta
		if (fpsAccumulator > 0.5f)
		{
			fpsAccumulator = 0f

			fps = (1f / frameDuration).toInt()
		}

        // limit fps
        sleep()
    }

    // ----------------------------------------------------------------------
    override fun pause() {}

    // ----------------------------------------------------------------------
    override fun resume() {}

    // ----------------------------------------------------------------------
    override fun hide() {}

    // ----------------------------------------------------------------------
    override fun dispose() {}

    //enregion
    //############################################################################
    //region InputProcessor

	// ----------------------------------------------------------------------
	override fun keyDown( keycode: Int ): Boolean
	{
		if (keycode == Input.Keys.GRAVE && !Global.release)
		{
			debugConsole.isVisible = !debugConsole.isVisible
			debugConsole.text.setKeyboardFocus(true)

			debugConsoleTable.toFront()

			return true
		}
		else
		{
			Global.controls.keyPressed(KeySource.KEYBOARD, keycode)
		}

		Global.controls.onInput(KeyMapping(KeySource.KEYBOARD, keycode))

		//val key = Global.controls.getKey(KeySource.KEYBOARD, keycode)
		//if (key != null) keyboardHelper?.keyDown(key)

		return false
	}

    // ----------------------------------------------------------------------
    override fun keyUp( keycode: Int ) = false

    // ----------------------------------------------------------------------
    override fun keyTyped( character: Char ) = false

    // ----------------------------------------------------------------------
    override fun touchDown( screenX: Int, screenY: Int, pointer: Int, button: Int ) = false

    // ----------------------------------------------------------------------
    override fun touchUp( screenX: Int, screenY: Int, pointer: Int, button: Int ) = false

    // ----------------------------------------------------------------------
    override fun touchDragged( screenX: Int, screenY: Int, pointer: Int ) = false

    // ----------------------------------------------------------------------
    override fun mouseMoved( screenX: Int, screenY: Int ) = false

    // ----------------------------------------------------------------------
    override fun scrolled(amount: Int) = false

    //endregion
    //############################################################################
    //region Methods

    // ----------------------------------------------------------------------
    fun baseCreate()
	{
        stage = Stage(ScalingViewport(Scaling.fit, Global.resolution.x.toFloat(), Global.resolution.y.toFloat()), SpriteBatch())

        mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

		if (!Global.release)
		{
			debugConsoleTable.setFillParent(true)
			stage.addActor(debugConsoleTable)

			debugConsole = DebugConsole(this.javaClass.simpleName)
			debugConsoleTable.add(debugConsole).width(300f).expand().left().top().pad(5f)

			debugConsole.register("CurrentScreen", "", { args, console ->
				console.write(this.javaClass.simpleName)

				true
			})

			debugConsole.isVisible = false
		}

        inputMultiplexer = InputMultiplexer()

        val inputProcessorOne = this
        val inputProcessorTwo = stage

        inputMultiplexer.addProcessor(inputProcessorTwo)
        inputMultiplexer.addProcessor(inputProcessorOne)

        create()
    }

    // ----------------------------------------------------------------------
    fun sleep() {
		diff = System.currentTimeMillis() - start
        if ( Global.fps > 0 ) {

            val targetDelay = 1000 / Global.fps
            if ( diff < targetDelay ) {
                try {
                    Thread.sleep(targetDelay - diff)
                } catch (e: InterruptedException) {
                }
            }
        }
		start = System.currentTimeMillis()

		if (frametime == -1f)
		{
			frametime = 1f / diff
		}
		else
		{
			frametime = (frametime + 1f/diff) / 2f
		}
    }

    //endregion
    //############################################################################
    //region Data

    var created: Boolean = false

    lateinit var stage: Stage
    lateinit var mainTable: Table

    lateinit var inputMultiplexer: InputMultiplexer

    var diff: Long = 0
    var start: Long = System.currentTimeMillis()
	var frametime: Float = -1f
	var frameDuration: Float = 0f
	var fps: Int = 0
	var fpsAccumulator: Float = 0f

	var debugAccumulator: Float = 0f

	private var fadeAccumulator: Float = 0f
	private var fadeTime: Float = 0f
	private var fadeType: FadeType = FadeType.IN
	private val fadeRenderer: ShapeRenderer = ShapeRenderer(8)

	val debugConsoleTable = Table()
	lateinit var debugConsole: DebugConsole

    //endregion
    //############################################################################
}

public enum class FadeType
{
	OUT,
	IN
}