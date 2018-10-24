package com.lyeeedar.Board

import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.Array2D

abstract class Creature(maxHp: Int, size: Int, override var sprite: Sprite, death: ParticleEffect) : Damageable()
{
	var size = 2
		set(value)
		{
			field = value
			tiles = Array2D(size, size){ x, y -> Tile(0, 0) }
		}

	lateinit var tiles: Array2D<Tile>

	var death: ParticleEffect

	init
	{
		this.maxhp = maxHp
		this.size = size
		this.death = death
	}

	val queuedTileSprites = com.badlogic.gdx.utils.Array<Renderable>()
	fun setTile(target: Tile, grid: Grid, delay: Float = 0f)
	{
		for (tile in tiles)
		{
			tile.monster = null
		}
		for (x in 0 until size)
		{
			for (y in 0 until size)
			{
				val tile = grid.tile(target.x + x, target.y + y)!!

				if (tile.orb != null)
				{
					val orb = tile.orb!!

					val sprite = orb.desc.death.copy()
					sprite.renderDelay = delay
					sprite.colour = orb.sprite.colour

					tile.effects.add(sprite)
				}

				tile.creature = this
				tiles[x, y] = tile
			}
		}

		for (effect in queuedTileSprites)
		{
			effect.size[0] = size
			effect.size[1] = size
			tiles[0, 0].effects.add(effect)
		}
		queuedTileSprites.clear()
	}

	abstract fun onTurn(grid: Grid)

	fun getBorderTiles(grid: Grid, range: Int = 1): Sequence<Tile>
	{
		fun isBorder(tile: Tile) = tiles.map { it.taxiDist(tile) }.min()!! <= range
		return grid.grid.filter(::isBorder)
	}
}