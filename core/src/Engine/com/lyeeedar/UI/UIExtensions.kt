package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Statics
import ktx.actors.alpha
import ktx.actors.plus
import ktx.actors.then
import ktx.scene2d.label
import ktx.scene2d.table

fun createGreyoutTable(stage: Stage, appearSpeed: Float = 0.1f, opacity: Float = 0.9f): Table
{
	val greyoutTable = Table()
	greyoutTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("white")).tint(Color(0f, 0f, 0f, opacity))
	greyoutTable.touchable = Touchable.enabled
	greyoutTable.setFillParent(true)
	greyoutTable.addAction(Actions.fadeIn(appearSpeed))

	stage.addActor(greyoutTable)

	return greyoutTable
}

fun Actor.fadeOutAndRemove(duration: Float)
{
	this.addAction(Actions.fadeOut(duration) then Actions.removeActor())
}

fun Drawable.setNoMinSize(): Drawable
{
	this.minWidth = 0f
	this.minHeight = 0f
	return this
}

fun showFullscreenText(text: String, minDuration: Float, exitAction: ()->Unit)
{
	val fadeTable = table {
		label(text, "default", Statics.skin) {
			cell -> cell.top()
		}
	}

	fadeTable.background = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/white.png")).tint(Color(0f, 0f, 0f, 0.7f))
	fadeTable.alpha = 0f

	val sequence = Actions.alpha(0f) then Actions.fadeIn(0.2f) then lambda {
		val outsequence = Actions.fadeOut(0.2f) then Actions.removeActor()

		Future.call({
			Statics.controls.onInput += fun(key): Boolean {
				fadeTable + outsequence
				exitAction.invoke()
				return true
			}
		}, minDuration)
	}

	fadeTable + sequence

	Statics.stage.addActor(fadeTable)
	fadeTable.setFillParent(true)
}

fun Batch.setColor(col: Colour)
{
	this.setColor(col.toFloatBits())
}

fun Actor.tint(col: Color): Actor
{
	this.color = col
	return this
}

fun Label.wrap(): Label
{
	this.setWrap(true)
	return this
}

fun Label.align(alignment: Int): Label
{
	this.setAlignment(alignment)
	return this
}

fun <T : Actor> Stack.addTable(actor: T): Cell<T>
{
	val holderTable = Table()
	val cell = holderTable.add(actor)
	this.add(holderTable)
	return cell
}