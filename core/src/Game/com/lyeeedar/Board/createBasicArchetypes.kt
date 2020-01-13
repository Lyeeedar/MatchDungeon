package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.lyeeedar.Board.CompletionCondition.CompletionConditionTurns
import com.lyeeedar.Components.*
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Statics
import com.lyeeedar.Util.tryGet

fun createBlock(theme: Theme, hp: Int): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.BLOCK)

	val position = PositionComponent.obtain()

	val renderable = RenderableComponent.obtain().set(theme.blockSprites.tryGet(0).copy())

	val damageableComponent = DamageableComponent.obtain()
	damageableComponent.deathEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()
	damageableComponent.alwaysShowHP = false
	damageableComponent.maxhp = hp

	val tutorialComponent = TutorialComponent.obtain()
	tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
		if (!Statics.settings.get("Block", false) )
		{
			val tutorial = Tutorial("Block")
			tutorial.addPopup("This is a block. Match in the tiles surrounding it to break it.", gridWidget.getRect(entity.pos().tile!!, true))

			return tutorial
		}

		return null
	}

	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(damageableComponent)
	entity.add(tutorialComponent)

	return entity
}

fun createContainer(sprite: Renderable, hp: Int, contents: Entity): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.CONTAINER)

	val position = PositionComponent.obtain()

	val renderable = RenderableComponent.obtain().set(sprite.copy())

	val damageableComponent = DamageableComponent.obtain()
	damageableComponent.deathEffect = AssetManager.loadParticleEffect("Hit").getParticleEffect()
	damageableComponent.alwaysShowHP = false
	damageableComponent.maxhp = hp

	val containerComponent = ContainerComponent.obtain()
	containerComponent.containedEntity = contents

	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(damageableComponent)
	entity.add(containerComponent)

	return entity
}

fun createChest(spawnOrbs: Boolean, theme: Theme): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.CHEST)

	val position = PositionComponent.obtain()

	val fullSprite = theme.chestFull.copy()
	val emptySprite = theme.chestEmpty.copy()
	val renderable = RenderableComponent.obtain()
	renderable.renderable = emptySprite

	val spawner = OrbSpawnerComponent.obtain()
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
				return createSinkable(theme.coin.copy())
			}
		}
		else
		{
			if (spawner.numToSpawn <= 0) return null
			spawner.numToSpawn--
			return createSinkable(theme.coin.copy())
		}
	}
	spawner.numToSpawnChanged += fun (numToSpawn: Int): Boolean {
		renderable.renderable = if (numToSpawn > 0) fullSprite else emptySprite
		return false
	}

	val tutorialComponent = TutorialComponent.obtain()
	tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
		if (spawner.numToSpawn > 0 && !Statics.settings.get("Chest", false))
		{
			val tutorial = Tutorial("Chest")
			tutorial.addPopup("This is a chest. Match in the tiles beneath this to spawn coins. When there are no more coins to spawn, it will appear empty.", gridWidget.getRect(entity))
			return tutorial
		}

		return null
	}


	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(spawner)
	entity.add(tutorialComponent)

	return entity
}

fun createSinkable(renderable: Renderable): Entity
{
	val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.SINKABLE)

	val position = PositionComponent.obtain()

	val renderable = RenderableComponent.obtain().set(renderable)

	val sinkable = SinkableComponent.obtain()

	val swappable = SwappableComponent.obtain()

	val tutorialComponent = TutorialComponent.obtain()
	tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
		if (!Statics.settings.get("Sinkable", false) )
		{
			val tutorial = Tutorial("Sinkable")
			tutorial.addPopup("This is a sinkable item. If you move it to the bottom of the board you will successfully sink it.", gridWidget.getRect(entity.pos().tile!!, true))
			return tutorial
		}

		return null
	}

	val entity = EntityPool.obtain()
	entity.add(archetype)
	entity.add(position)
	entity.add(renderable)
	entity.add(sinkable)
	entity.add(swappable)
	entity.add(tutorialComponent)

	return entity
}