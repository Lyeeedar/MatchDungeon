package com.lyeeedar.Screens

import com.badlogic.gdx.*
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.input.GestureDetector
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScalingViewport
import com.lyeeedar.Game.Save
import com.lyeeedar.UI.DebugConsole
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.KeyMapping
import com.lyeeedar.Util.KeySource
import com.lyeeedar.Util.Statics
import ktx.actors.setKeyboardFocus


/**
 * Created by Philip on 20-Mar-16.
 */

abstract class AbstractScreen() : Screen, InputProcessor, GestureDetector.GestureListener
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
		Statics.game.switchScreen(this)
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

		Save.save()
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
		Tutorial.current?.act(delta)

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
		if (keycode == Input.Keys.GRAVE && !Statics.release)
		{
			debugConsole.isVisible = !debugConsole.isVisible
			debugConsole.text.setKeyboardFocus(true)

			debugConsoleTable.toFront()

			return true
		}
		else
		{
			Statics.controls.keyPressed(KeySource.KEYBOARD, keycode)
		}

		Statics.controls.onInput(KeyMapping(KeySource.KEYBOARD, keycode))

		//val key = Statics.controls.getKey(KeySource.KEYBOARD, keycode)
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
	override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

	// ----------------------------------------------------------------------
    override fun scrolled(amount: Int) = false

	// ----------------------------------------------------------------------
	override fun tap(x: Float, y: Float, count: Int, button: Int): Boolean = false

	// ----------------------------------------------------------------------
	override fun longPress(x: Float, y: Float): Boolean = false

	// ----------------------------------------------------------------------
	/** Called when the user dragged a finger over the screen and lifted it. Reports the last known velocity of the finger in
	 * pixels per second.
	 * @param velocityX velocity on x in seconds
	 * @param velocityY velocity on y in seconds
	 */
	override fun fling(velocityX: Float, velocityY: Float, button: Int): Boolean
	{
		if (!Statics.release)
		{
			debugConsole.isVisible = !debugConsole.isVisible
			debugConsole.text.setKeyboardFocus(true)

			debugConsoleTable.toFront()

			return true
		}

		return false
	}

	// ----------------------------------------------------------------------
	/** Called when the user drags a finger over the screen.
	 * @param deltaX the difference in pixels to the last drag event on x.
	 * @param deltaY the difference in pixels to the last drag event on y.
	 */
	override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean = false

	// ----------------------------------------------------------------------
	/** Called when no longer panning.  */
	override fun panStop(x: Float, y: Float, pointer: Int, button: Int): Boolean = false

	// ----------------------------------------------------------------------
	/** Called when the user performs a pinch zoom gesture. The original distance is the distance in pixels when the gesture
	 * started.
	 * @param initialDistance distance between fingers when the gesture started.
	 * @param distance current distance between fingers.
	 */
	override fun zoom(initialDistance: Float, distance: Float): Boolean = false

	// ----------------------------------------------------------------------
	/** Called when a user performs a pinch zoom gesture. Reports the initial positions of the two involved fingers and their
	 * current positions.
	 * @param initialPointer1
	 * @param initialPointer2
	 * @param pointer1
	 * @param pointer2
	 */
	override fun pinch(initialPointer1: Vector2, initialPointer2: Vector2, pointer1: Vector2, pointer2: Vector2): Boolean = false

	// ----------------------------------------------------------------------
	/** Called when no longer pinching.  */
	override fun pinchStop() { }

	//endregion
    //############################################################################
    //region Methods

    // ----------------------------------------------------------------------
    fun baseCreate()
	{
        stage = Stage(ScalingViewport(Scaling.fit, Statics.resolution.x.toFloat(), Statics.resolution.y.toFloat()), SpriteBatch())

        mainTable = Table()
        mainTable.setFillParent(true)
        stage.addActor(mainTable)

		if (!Statics.release)
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

		val gestureProcess = GestureDetector(this)
        val inputProcessorOne = this
        val inputProcessorTwo = stage

        inputMultiplexer.addProcessor(inputProcessorTwo)
		inputMultiplexer.addProcessor(gestureProcess)
        inputMultiplexer.addProcessor(inputProcessorOne)

        create()
    }

    // ----------------------------------------------------------------------
    fun sleep() {
		diff = System.currentTimeMillis() - start
        if ( Statics.fps > 0 ) {

            val targetDelay = 1000 / Statics.fps
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