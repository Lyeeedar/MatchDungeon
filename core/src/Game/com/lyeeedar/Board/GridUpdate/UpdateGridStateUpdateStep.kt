package com.lyeeedar.Board.GridUpdate

import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Board.Tile
import com.lyeeedar.Components.*
import com.lyeeedar.Systems.GridSystem

class UpdateGridStateUpdateStep : AbstractUpdateStep()
{
	val chestTiles = Array<Tile>()
	val sinkPathSet = ObjectSet<Tile>()

	override fun doUpdate(gridSystem: GridSystem): Boolean
	{
		val grid = gridSystem.grid!!

		grid.monsterTiles.clear()
		grid.friendlyTiles.clear()
		grid.sinkableTiles.clear()
		grid.breakableTiles.clear()
		grid.sinkPathTiles.clear()
		grid.notSinkPathTiles.clear()
		grid.basicOrbTiles.clear()
		grid.attackTiles.clear()
		grid.namedOrbTiles.clear()

		chestTiles.clear()

		for (x in 0 until grid.width)
		{
			for (y in 0 until grid.height)
			{
				val tile = grid.grid[x, y]

				val contents = tile.contents ?: continue

				val ai = contents.ai()
				val healable = contents.healable()
				val damageable = contents.damageable()
				val sinkable = contents.sinkable()
				val matchable = contents.matchable()
				val special = contents.special()
				val orbSpawner = contents.orbSpawner()
				val monsterEffect = contents.monsterEffect()

				if (ai != null && damageable != null)
				{
					// monster
					grid.monsterTiles.add(tile)
				}
				if (healable != null)
				{
					// friendly
					grid.friendlyTiles.add(tile)
				}
				if (sinkable != null)
				{
					// sinkable
					grid.sinkableTiles.add(tile)
				}
				if (ai == null && damageable != null)
				{
					// block / container
					grid.breakableTiles.add(tile)
				}
				if (matchable != null && special == null)
				{
					// orb or suchlike, excluding specials
					if (matchable.desc.isNamed)
					{
						grid.namedOrbTiles.add(tile)
					}
					else
					{
						grid.basicOrbTiles.add(tile)
					}
				}
				if (orbSpawner != null && orbSpawner.numToSpawn > 0)
				{
					chestTiles.add(tile)
				}
				if (monsterEffect != null)
				{
					grid.attackTiles.add(tile)
				}
			}
		}

		chestTiles.addAll(grid.sinkableTiles)
		for (sourceTile in chestTiles)
		{
			// for each sinkable tile, read down to the bottom, and add all matchable tiles to the list
			for (y in sourceTile.y until grid.height-1)
			{
				val tile = grid.grid[sourceTile.x, y]

				val contents = tile.contents ?: continue
				val matchable = contents.matchable() ?: continue
				val special = contents.special()

				if (special == null)
				{
					grid.sinkPathTiles.add(tile)
				}
			}
		}

		sinkPathSet.clear()
		sinkPathSet.addAll(grid.sinkPathTiles)
		for (tile in grid.grid)
		{
			if (!sinkPathSet.contains(tile))
			{
				grid.notSinkPathTiles.add(tile)
			}
		}

		return true
	}

	override fun doTurn(gridSystem: GridSystem)
	{

	}
}