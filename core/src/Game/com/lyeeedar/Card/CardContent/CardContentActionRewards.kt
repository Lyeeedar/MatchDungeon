package com.lyeeedar.Card.CardContent

import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Direction
import com.lyeeedar.Game.AbstractReward
import com.lyeeedar.Screens.CardScreen
import com.lyeeedar.UI.*
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Future
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.XmlData
import ktx.actors.then
import ktx.collections.toGdxArray

class CardContentActionRewards : AbstractCardContentAction()
{
	val rewards = Array<AbstractReward>()

	override fun parse(xmlData: XmlData)
	{
		for (el in xmlData.children)
		{
			rewards.add(AbstractReward.load(el))
		}
	}

	var displayingRewards = false
	var awaitingAdvance = false

	override fun advance(CardContent: CardContent, CardContentScreen: CardScreen): Boolean
	{
		if (!displayingRewards && !awaitingAdvance)
		{
			displayingRewards = true

			val chosenRewards = rewards.filter { it.chance.evaluate() }.flatMap { it.reward() }.toGdxArray()

			displayRewards(chosenRewards) {
				awaitingAdvance = true
				CardContent.CardContentStack.last().index++
				CardContentScreen.advanceContent()
			}
		}

		val complete = displayingRewards && awaitingAdvance
		if (complete)
		{
			displayingRewards = false
			awaitingAdvance = false
		}

		return complete
	}

	private fun displayRewards(cards: Array<CardWidget>, onCompleteAction: ()->Unit)
	{
		val greyoutTable = createGreyoutTable(Statics.stage)

		val cardsTable = Table()
		greyoutTable.add(cardsTable).grow()
		greyoutTable.row()

		val buttonTable = Table()
		greyoutTable.add(buttonTable).growX()

		var gatheredLoot = 0
		for (card in cards)
		{
			val oldPickFuns = card.pickFuns.toGdxArray()
			card.pickFuns.clear()

			for (oldPick in oldPickFuns)
			{
				card.addPick(oldPick.string) {
					card.dissolve(CardWidget.DissolveType.HOLY, 1f, 6f)

					card.isPicked = true
					oldPick.pickFun(card)

					gatheredLoot++
					if (gatheredLoot == cards.size)
					{
						greyoutTable.fadeOutAndRemove(0.6f)

						onCompleteAction()
					}
				}
			}
		}

		val chestClosed = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold"), 128f, 128f)
		val chestOpen = SpriteWidget(AssetManager.loadSprite("Oryx/uf_split/uf_items/chest_gold_open"), 128f, 128f)

		val wobbleDuration = 1f
		chestClosed.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
		chestClosed.setSize(128f, 128f)
		chestClosed.addAction(WobbleAction(0f, 35f, 0.1f, wobbleDuration))
		Statics.stage.addActor(chestClosed)

		Future.call(
			{
				chestClosed.remove()

				chestOpen.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
				chestOpen.setSize(128f, 128f)
				Statics.stage.addActor(chestOpen)

				val effect = AssetManager.loadParticleEffect("ChestOpen").getParticleEffect()
				val particleActor = ParticleEffectActor(effect, true)
				particleActor.setSize(128f, 128f)
				particleActor.setPosition(Statics.stage.width / 2f - 64f, Statics.stage.height / 2f - 64f)
				Statics.stage.addActor(particleActor)

				Future.call(
					{
						val sequence = Actions.delay(0.2f) then Actions.fadeOut(0.3f) then Actions.removeActor()
						chestOpen.addAction(sequence)

						for (card in cards)
						{
							Statics.stage.addActor(card)
						}

						CardWidget.layoutCards(cards, Direction.CENTER, cardsTable, startScale = 0.3f, flip = true)
					}, 0.4f)
			}, wobbleDuration)
	}

	override fun resolve(nodeMap: ObjectMap<String, CardContentNode>)
	{

	}
}

