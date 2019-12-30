package com.lyeeedar.Util

import com.badlogic.gdx.Input
import com.badlogic.gdx.utils.ObjectSet


class Controls
{
	enum class Keys
	{
		LEFT,
		RIGHT,
		UP,
		DOWN,
		CANCEL,
		ACCEPT,
		WAIT,
		DEFENSE,
		ATTACKNORMAL,
		ATTACKSPECIAL,
		MENU,
		INFO,

		TOUCHED
	}

	private val keyMap = FastEnumMap<Keys, ObjectSet<KeyMapping>>(Keys::class.java)

	private val keyDownMap = FastEnumMap<Keys, Boolean>(Keys::class.java)
	private val keyPressMap = FastEnumMap<Keys, Boolean>(Keys::class.java)

	val onInput = Event1Arg<KeyMapping>()

	init
	{
		for (key in Keys.values())
		{
			keyPressMap[key] = false
			keyDownMap[key] = false
			keyMap[key] = ObjectSet()
		}

		defaultArrow()
		defaultWASD()
		defaultNumPad()
		defaultXboxController()

		addKeyMapping(Keys.TOUCHED, KeySource.MOUSE, 0)
	}

	fun addKeyMapping(key: Keys, source: KeySource, code: Int)
	{
		keyMap[key].add(KeyMapping(source, code))
	}

	fun defaultXboxController()
	{
		addKeyMapping(Keys.LEFT, KeySource.CONTROLLERDPAD, Xbox360Controller.BUTTON_DPAD_WEST)
		addKeyMapping(Keys.RIGHT, KeySource.CONTROLLERDPAD, Xbox360Controller.BUTTON_DPAD_EAST)
		addKeyMapping(Keys.UP, KeySource.CONTROLLERDPAD, Xbox360Controller.BUTTON_DPAD_NORTH)
		addKeyMapping(Keys.DOWN, KeySource.CONTROLLERDPAD, Xbox360Controller.BUTTON_DPAD_SOUTH)

		addKeyMapping(Keys.LEFT, KeySource.CONTROLLERSTICK, Xbox360Controller.AXIS_LEFT_X_N)
		addKeyMapping(Keys.RIGHT, KeySource.CONTROLLERSTICK, Xbox360Controller.AXIS_LEFT_X_P)
		addKeyMapping(Keys.UP, KeySource.CONTROLLERSTICK, Xbox360Controller.AXIS_LEFT_Y_P)
		addKeyMapping(Keys.DOWN, KeySource.CONTROLLERSTICK, Xbox360Controller.AXIS_LEFT_Y_N)

		addKeyMapping(Keys.CANCEL, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_B)
		addKeyMapping(Keys.ACCEPT, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_A)
		addKeyMapping(Keys.WAIT, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_A)
		addKeyMapping(Keys.DEFENSE, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_B)
		addKeyMapping(Keys.ATTACKNORMAL, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_X)
		addKeyMapping(Keys.ATTACKSPECIAL, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_Y)
		addKeyMapping(Keys.MENU, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_START)
		addKeyMapping(Keys.INFO, KeySource.CONTROLLERBUTTON, Xbox360Controller.BUTTON_BACK)
	}

	fun defaultArrow()
	{
		addKeyMapping(Keys.LEFT, KeySource.KEYBOARD, Input.Keys.LEFT)
		addKeyMapping(Keys.RIGHT, KeySource.KEYBOARD, Input.Keys.RIGHT)
		addKeyMapping(Keys.UP, KeySource.KEYBOARD, Input.Keys.UP)
		addKeyMapping(Keys.DOWN, KeySource.KEYBOARD, Input.Keys.DOWN)
		addKeyMapping(Keys.CANCEL, KeySource.KEYBOARD, Input.Keys.ESCAPE)
		addKeyMapping(Keys.ACCEPT, KeySource.KEYBOARD, Input.Keys.ENTER)
		addKeyMapping(Keys.WAIT, KeySource.KEYBOARD, Input.Keys.SPACE)
		addKeyMapping(Keys.DEFENSE, KeySource.KEYBOARD, Input.Keys.SHIFT_RIGHT)
		addKeyMapping(Keys.ATTACKNORMAL, KeySource.KEYBOARD, Input.Keys.CONTROL_RIGHT)
		addKeyMapping(Keys.ATTACKSPECIAL, KeySource.KEYBOARD, Input.Keys.NUMPAD_0)
		addKeyMapping(Keys.MENU, KeySource.KEYBOARD, Input.Keys.ESCAPE)
		addKeyMapping(Keys.INFO, KeySource.KEYBOARD, Input.Keys.SLASH)
	}

	fun defaultWASD()
	{
		addKeyMapping(Keys.LEFT, KeySource.KEYBOARD, Input.Keys.A)
		addKeyMapping(Keys.RIGHT, KeySource.KEYBOARD, Input.Keys.D)
		addKeyMapping(Keys.UP, KeySource.KEYBOARD, Input.Keys.W)
		addKeyMapping(Keys.DOWN, KeySource.KEYBOARD, Input.Keys.S)
		addKeyMapping(Keys.CANCEL, KeySource.KEYBOARD, Input.Keys.ESCAPE)
		addKeyMapping(Keys.ACCEPT, KeySource.KEYBOARD, Input.Keys.ENTER)
		addKeyMapping(Keys.WAIT, KeySource.KEYBOARD, Input.Keys.SPACE)
		addKeyMapping(Keys.DEFENSE, KeySource.KEYBOARD, Input.Keys.SHIFT_LEFT)
		addKeyMapping(Keys.ATTACKNORMAL, KeySource.KEYBOARD, Input.Keys.Q)
		addKeyMapping(Keys.ATTACKSPECIAL, KeySource.KEYBOARD, Input.Keys.E)
		addKeyMapping(Keys.MENU, KeySource.KEYBOARD, Input.Keys.ESCAPE)
		addKeyMapping(Keys.INFO, KeySource.KEYBOARD, Input.Keys.SLASH)
	}

	fun defaultNumPad()
	{
		addKeyMapping(Keys.LEFT, KeySource.KEYBOARD, Input.Keys.NUMPAD_4)
		addKeyMapping(Keys.RIGHT, KeySource.KEYBOARD, Input.Keys.NUMPAD_6)
		addKeyMapping(Keys.UP, KeySource.KEYBOARD, Input.Keys.NUMPAD_8)
		addKeyMapping(Keys.DOWN, KeySource.KEYBOARD, Input.Keys.NUMPAD_2)
		addKeyMapping(Keys.CANCEL, KeySource.KEYBOARD, Input.Keys.PERIOD)
		addKeyMapping(Keys.ACCEPT, KeySource.KEYBOARD, Input.Keys.ENTER)
		addKeyMapping(Keys.WAIT, KeySource.KEYBOARD, Input.Keys.NUMPAD_5)
		addKeyMapping(Keys.DEFENSE, KeySource.KEYBOARD, Input.Keys.NUMPAD_1)
		addKeyMapping(Keys.ATTACKNORMAL, KeySource.KEYBOARD, Input.Keys.NUMPAD_7)
		addKeyMapping(Keys.ATTACKSPECIAL, KeySource.KEYBOARD, Input.Keys.NUMPAD_9)
		addKeyMapping(Keys.MENU, KeySource.KEYBOARD, Input.Keys.ESCAPE)
		addKeyMapping(Keys.INFO, KeySource.KEYBOARD, Input.Keys.SLASH)
	}

	fun setKeyMap(key: Keys, source: KeySource, keycode: Int)
	{
		addKeyMapping(key, source, keycode)
	}

	fun getKeyCodes(key: Keys): ObjectSet<KeyMapping>
	{
		return keyMap.get(key)
	}

	fun getKey(source: KeySource, keycode: Int): Keys?
	{
		for (key in Keys.values())
		{
			if (keyMap[key].any { it.source == source && it.code == keycode }) return key
		}

		return null
	}

	fun isKey(key: Keys, source: KeySource, keycode: Int): Boolean
	{
		return keyMap.get(key).any { it.source == source && it.code == keycode }
	}

	fun isKeyDown(key: Keys): Boolean
	{
		return keyDownMap[key]
	}

	fun isKeyDownAndNotConsumed(key: Keys): Boolean
	{
		return keyPressMap[key]
	}

	fun keyPressed(source: KeySource, code: Int)
	{
		for (k in Keys.values())
		{
			if (keyMap[k].any { it.source == source && it.code == code })
			{
				keyPressMap[k] = true
				keyDownMap[k] = true
			}
		}
	}

	fun keyReleased(source: KeySource, code: Int)
	{
		for (k in Keys.values())
		{
			if (keyMap[k].any { it.source == source && it.code == code })
			{
				keyPressMap[k] = false
				keyDownMap[k] = false
			}
		}
	}

	fun consumeKeyPress(key: Keys): Boolean
	{
		val pressed = keyPressMap[key] ?: false
		keyPressMap[key] = false

		return pressed
	}

	fun isDirectionDownAndNotConsumed(): Boolean = keyPressMap[Keys.UP] || keyPressMap[Keys.DOWN] || keyPressMap[Keys.LEFT] || keyPressMap[Keys.RIGHT]

	fun isDirectionDown(): Boolean = Keys.UP.isDown() || Keys.DOWN.isDown() || Keys.LEFT.isDown() || Keys.RIGHT.isDown()
}

fun Controls.Keys.isDown() = Statics.controls.isKeyDown(this)
fun Controls.Keys.isDownAndNotConsumed() = Statics.controls.isKeyDownAndNotConsumed(this)
fun Controls.Keys.consumePress() = Statics.controls.consumeKeyPress(this)

enum class KeySource
{
	KEYBOARD,
	CONTROLLERBUTTON,
	CONTROLLERSTICK,
	CONTROLLERDPAD,
	MOUSE
}
data class KeyMapping(val source: KeySource, val code: Int)

object Xbox360Controller
{
	val N_BUTTONS = 10
	val N_TRIGGERS = 2
	val N_DPAD = 8
	val N_AXIS = 4

	// Xbox Controller Wrapper mappings
	val BUTTON_DPAD_NORTH = 0
	val BUTTON_DPAD_NORTHEAST = 1
	val BUTTON_DPAD_EAST = 2
	val BUTTON_DPAD_SOUTHEAST = 3
	val BUTTON_DPAD_SOUTH = 4
	val BUTTON_DPAD_SOUTHWEST = 5
	val BUTTON_DPAD_WEST = 6
	val BUTTON_DPAD_NORTHWEST = 7

	// Xbox Controller mappings
	val BUTTON_A = 0
	val BUTTON_B = 1
	val BUTTON_X = 2
	val BUTTON_Y = 3
	val BUTTON_BACK = 6
	val BUTTON_START = 7
	val BUTTON_LB = 4
	val BUTTON_L3 = 8
	val BUTTON_RB = 5
	val BUTTON_R3 = 9
	val AXIS_LEFT_X = 1
	val AXIS_LEFT_Y = 0
	val AXIS_LEFT_TRIGGER = 4
	val AXIS_RIGHT_X = 3
	val AXIS_RIGHT_Y = 2
	val AXIS_RIGHT_TRIGGER = 4

	// Axis
	val AXIS_LEFT_X_N = 0
	val AXIS_LEFT_X_P = 1
	val AXIS_LEFT_Y_N = 2
	val AXIS_LEFT_Y_P = 3
}