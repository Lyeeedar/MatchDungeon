package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions.*
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics
import ktx.actors.alpha
import ktx.actors.then

class TutorialPopup(val text: String, val emphasisSource: Any, val advance: () -> Unit) : Table()
{
	fun evaluateBounds(): Rectangle
	{
		return when (emphasisSource)
		{
			is Rectangle -> emphasisSource
			is Actor -> emphasisSource.getBounds()
			//is Array<*> -> GridWidget.instance.getRect(emphasisSource as Array<Point>)
			else -> Rectangle(Statics.stage.width / 2f, Statics.stage.height / 2f, 0f, 0f)
		}
	}

	fun show(greyoutOnEnter: Boolean, clearOnExit: Boolean)
	{
		val emphasis = evaluateBounds()

		if (emphasis.width > 0f && emphasis.height > 0f)
		{
			val pad = 5f
			emphasis.x -= pad
			emphasis.y -= pad
			emphasis.width += pad * 2f
			emphasis.height += pad * 2f
		}

		val animSpeed = 0.3f

		val greyoutalpha = 0.9f

		// add greyout

		if (greyoutOnEnter)
		{
			topGreyout = Table()
			topGreyout.alpha = 0f
			topGreyout.touchable = Touchable.enabled
			topGreyout.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, greyoutalpha))
			topGreyout.setBounds(0f, 0f, Statics.stage.width, emphasis.y)
			topGreyout.addAction(alpha(0f) then fadeIn(animSpeed))
			Statics.stage.addActor(topGreyout)

			bottomGreyout = Table()
			bottomGreyout.alpha = 0f
			bottomGreyout.touchable = Touchable.enabled
			bottomGreyout.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, greyoutalpha))
			bottomGreyout.setBounds(0f, emphasis.y + emphasis.height, Statics.stage.width, Statics.stage.height - (emphasis.y + emphasis.height))
			bottomGreyout.addAction(alpha(0f) then fadeIn(animSpeed))
			Statics.stage.addActor(bottomGreyout)

			leftGreyout = Table()
			leftGreyout.alpha = 0f
			leftGreyout.touchable = Touchable.enabled
			leftGreyout.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, greyoutalpha))
			leftGreyout.setBounds(0f, emphasis.y, emphasis.x, emphasis.height)
			leftGreyout.addAction(alpha(0f) then fadeIn(animSpeed))
			Statics.stage.addActor(leftGreyout)

			rightGreyout = Table()
			rightGreyout.alpha = 0f
			rightGreyout.touchable = Touchable.enabled
			rightGreyout.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, greyoutalpha))
			rightGreyout.setBounds(emphasis.x + emphasis.width, emphasis.y, Statics.stage.width - (emphasis.x + emphasis.width), emphasis.height)
			rightGreyout.addAction(alpha(0f) then fadeIn(animSpeed))
			Statics.stage.addActor(rightGreyout)

			centerBlock = Table()
			centerBlock.alpha = 0f
			centerBlock.touchable = Touchable.enabled
			centerBlock.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("GUI/border"), 8, 8, 8, 8)).tint(Color.GOLD)
			centerBlock.setBounds(emphasis.x, emphasis.y, emphasis.width, emphasis.height)
			if (emphasis.width != 0f && emphasis.height != 0f) centerBlock.addAction(alpha(0f) then fadeIn(animSpeed))
			Statics.stage.addActor(centerBlock)
		}

		// add popup
		background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/background.png"), 24, 24, 24, 24)).tint(Color(1f, 1f, 1f, 0.7f))
		touchable = Touchable.enabled

		val label = Label(text, Statics.skin)
		label.setWrap(true)

		this.alpha = 0f
		add(label).grow().width(Statics.stage.width * 0.75f)

		addAction(alpha(0f) then fadeIn(animSpeed))

		val click = {
			clearListeners()
			topGreyout.clearListeners()
			bottomGreyout.clearListeners()
			leftGreyout.clearListeners()
			rightGreyout.clearListeners()
			centerBlock.clearListeners()

			addAction(fadeOut(0.1f) then removeActor())

			if (clearOnExit)
			{
				topGreyout.addAction(alpha(1f) then fadeOut(animSpeed) then removeActor())
				bottomGreyout.addAction(alpha(1f) then fadeOut(animSpeed) then removeActor())
				leftGreyout.addAction(alpha(1f) then fadeOut(animSpeed) then removeActor())
				rightGreyout.addAction(alpha(1f) then fadeOut(animSpeed) then removeActor())

				centerBlock.addAction(fadeOut(0.1f) then removeActor())
			}

			advance.invoke()
		}
		addClickListener(click)

		topGreyout.clearListeners()
		bottomGreyout.clearListeners()
		leftGreyout.clearListeners()
		rightGreyout.clearListeners()
		centerBlock.clearListeners()

		topGreyout.addClickListener(click)
		bottomGreyout.addClickListener(click)
		leftGreyout.addClickListener(click)
		rightGreyout.addClickListener(click)
		centerBlock.addClickListener(click)

		pack()

		val placeTop = emphasis.y > Statics.stage.height / 2f

		val px = (emphasis.x + emphasis.width / 2f) - width / 2f
		var py = 0f
		if (placeTop)
		{
			py = emphasis.y - height - 20
		}
		else
		{
			py = emphasis.y + emphasis.height + 20
		}

		setPosition(px, py)
		Statics.stage.addActor(this)
		ensureOnScreen(5f)
	}

	companion object
	{
		var topGreyout = Table()
		var bottomGreyout = Table()
		var leftGreyout = Table()
		var rightGreyout = Table()
		var centerBlock = Table()
	}
}