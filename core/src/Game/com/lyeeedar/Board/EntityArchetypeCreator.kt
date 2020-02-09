package com.lyeeedar.Board

import com.lyeeedar.Board.CompletionCondition.CompletionConditionTurns
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*

class EntityArchetypeCreator
{
	companion object
	{
		val blockBuilder = EntityArchetypeBuilder()
				.add(ComponentType.EntityArchetype)
				.add(ComponentType.Position)
				.add(ComponentType.Renderable)
				.add(ComponentType.Damageable)
				.add(ComponentType.Tutorial)
		fun createBlock(theme: Theme, hp: Int): Entity
		{
			val entity = blockBuilder.build()

			entity.archetype()!!.set(EntityArchetype.BLOCK)
			entity.renderable()!!.set(theme.data.blockSprites.tryGet(0).copy())

			val damageableComponent = entity.damageable()!!
			damageableComponent.deathEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()
			damageableComponent.alwaysShowHP = false
			damageableComponent.maxhp = hp

			val tutorialComponent = entity.tutorial()!!
			tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
				if (!Statics.settings.get("Block", false) )
				{
					val tutorial = Tutorial("Block")
					tutorial.addPopup(Localisation.getText("block.tutorial", "UI"), gridWidget.getRect(entity.pos()!!.tile!!, true))

					return tutorial
				}

				return null
			}

			return entity
		}

		val containerBuilder = EntityArchetypeBuilder()
				.add(ComponentType.EntityArchetype)
				.add(ComponentType.Position)
				.add(ComponentType.Renderable)
				.add(ComponentType.Damageable)
				.add(ComponentType.Container)
		fun createContainer(sprite: Renderable, hp: Int, contents: Entity): Entity
		{
			val entity = containerBuilder.build()

			entity.archetype()!!.set(EntityArchetype.CONTAINER)
			entity.renderable()!!.set(sprite.copy())

			val damageableComponent = entity.damageable()!!
			damageableComponent.deathEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()
			damageableComponent.alwaysShowHP = false
			damageableComponent.maxhp = hp

			val containerComponent = entity.container()!!
			containerComponent.containedEntity = contents

			return entity
		}

		val chestBuilder = EntityArchetypeBuilder()
				.add(ComponentType.EntityArchetype)
				.add(ComponentType.Position)
				.add(ComponentType.Renderable)
				.add(ComponentType.OrbSpawner)
				.add(ComponentType.Tutorial)
		fun createChest(spawnOrbs: Boolean, theme: Theme): Entity
		{
			val entity = chestBuilder.build()

			entity.archetype()!!.set(EntityArchetype.CHEST)

			val fullSprite = theme.data.chestFull.copy()
			val emptySprite = theme.data.chestEmpty.copy()

			val renderable = entity.renderable()!!
			renderable.renderable = emptySprite

			val spawner = entity.orbSpawner()!!
			spawner.canSpawnSinkables = true
			spawner.spawn = fun(grid: Grid, entity: Entity): Entity? {
				if (spawnOrbs)
				{
					if (spawner.numToSpawn <= 0) return grid.level.spawnOrb()

					// make sure we dont flood the board
					val coinsOnBoard = grid.grid.filter { it.contents?.sinkable() != null }.count()
					if (coinsOnBoard > 5) return grid.level.spawnOrb()

					var chosenSpacing = 3
					val turnsCondition = grid.level.defeatConditions.filterIsInstance<CompletionConditionTurns>().firstOrNull()
					if (turnsCondition != null)
					{
						if (turnsCondition.turnCount < turnsCondition.maxTurnCount / 4)
						{
							chosenSpacing = 0
						}
						else if (turnsCondition.turnCount < (turnsCondition.maxTurnCount / 4) * 2)
						{
							chosenSpacing = 1
						}
						else if (turnsCondition.turnCount < (turnsCondition.maxTurnCount / 4) * 3)
						{
							chosenSpacing = 2
						}
						else
						{
							chosenSpacing = 3
						}
					}

					if (spawner.spacingCounter < chosenSpacing)
					{
						spawner.spacingCounter++
						return grid.level.spawnOrb()
					}
					else
					{
						spawner.spacingCounter = 0
						spawner.numToSpawn--
						return createSinkable(theme.data.coin.copy())
					}
				}
				else
				{
					if (spawner.numToSpawn <= 0) return null
					spawner.numToSpawn--
					return createSinkable(theme.data.coin.copy())
				}
			}
			spawner.numToSpawnChanged += fun (numToSpawn: Int): HandlerAction {
				renderable.renderable = if (numToSpawn > 0) fullSprite else emptySprite
				return HandlerAction.KeepAttached
			}

			val tutorialComponent = entity.tutorial()!!
			tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
				if (spawner.numToSpawn > 0 && !Statics.settings.get("Chest", false))
				{
					val tutorial = Tutorial("Chest")
					tutorial.addPopup(Localisation.getText("chest.tutorial", "UI"), gridWidget.getRect(entity))
					return tutorial
				}

				return null
			}

			return entity
		}

		val sinkableBuilder = EntityArchetypeBuilder()
				.add(ComponentType.EntityArchetype)
				.add(ComponentType.Position)
				.add(ComponentType.Renderable)
				.add(ComponentType.Sinkable)
				.add(ComponentType.Swappable)
				.add(ComponentType.Tutorial)
		fun createSinkable(renderable: Renderable): Entity
		{
			val entity = sinkableBuilder.build()

			entity.archetype()!!.set(EntityArchetype.SINKABLE)
			entity.renderable()!!.set(renderable)

			val tutorialComponent = entity.tutorial()!!
			tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
				if (!Statics.settings.get("Sinkable", false) )
				{
					val tutorial = Tutorial("Sinkable")
					tutorial.addPopup(Localisation.getText("sinkable.tutorial", "UI"), gridWidget.getRect(entity.pos()!!.tile!!, true))
					return tutorial
				}

				return null
			}

			return entity
		}

		val orbBuilder = EntityArchetypeBuilder()
				.add(ComponentType.EntityArchetype)
				.add(ComponentType.Position)
				.add(ComponentType.Renderable)
				.add(ComponentType.Matchable)
				.add(ComponentType.Swappable)
				.add(ComponentType.Tutorial)
		fun createOrb(desc: OrbDesc): Entity
		{
			val entity = orbBuilder.build()

			entity.archetype()!!.set(EntityArchetype.ORB)
			entity.renderable()!!.set(desc.sprite.copy())

			val matchable = entity.matchable()!!
			val swappable = entity.swappable()!!

			val tutorialComponent = entity.tutorial()!!
			tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
				if (swappable.sealed && !Statics.settings.get("Seal", false) )
				{
					val tutorial = Tutorial("Seal")
					tutorial.addPopup(Localisation.getText("seal.tutorial", "UI"), gridWidget.getRect(entity))
					return tutorial
				}

				return null
			}

			matchable.setDesc(desc, entity)

			return entity
		}
	}
}