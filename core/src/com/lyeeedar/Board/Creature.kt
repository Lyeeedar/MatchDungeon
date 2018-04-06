package com.lyeeedar.Board

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Animation.BlinkAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.Array2D
import com.lyeeedar.Util.Colour

abstract class Creature(maxHp: Int, size: Int, sprite: Sprite, death: ParticleEffect)
{
	var damageReduction: Int = 0
		set(value)
		{
			field = value
			remainingReduction = value
		}
	var remainingReduction: Int = 0

	var hp: Int = 1
		set(value)
		{
			if (value < field)
			{
				val loss = field - value
				lostHP += loss

				sprite.colourAnimation = BlinkAnimation.obtain().set(Colour(Color.RED), sprite.colour, 0.15f, true)
			}

			field = value
			if (field < 0) field = 0
		}

	var lostHP: Int = 0

	var maxhp: Int = 1
		set(value)
		{
			field = value
			hp = value
		}

	var size = 2
		set(value)
		{
			field = value
			tiles = Array2D(size, size){ x, y -> Tile(0, 0) }
		}

	lateinit var tiles: Array2D<Tile>

	lateinit var sprite: Sprite
	lateinit var death: ParticleEffect

	val damSources = ObjectSet<Any>()

	init
	{
		this.maxhp = maxHp
		this.size = size
		this.sprite = sprite
		this.death = death
	}

	fun setTile(target: Tile, grid: Grid)
	{
		for (tile in tiles)
		{
			tile.monster = null
		}
		for (x in 0..size-1)
		{
			for (y in 0..size - 1)
			{
				val tile = grid.tile(target.x + x, target.y + y)!!

				if (tile.orb != null)
				{
					val orb = tile.orb!!

					val sprite = orb.desc.death.copy()
					sprite.colour = orb.sprite.colour

					tile.effects.add(sprite)
				}

				tile.creature = this
				tiles[x, y] = tile
			}
		}
	}

	abstract fun onTurn(grid: Grid)

	fun getBorderTiles(grid: Grid, range: Int = 1): Sequence<Tile>
	{
		fun isBorder(tile: Tile) = tiles.map { it.dist(tile) }.min()!! <= range
		return grid.grid.filter(::isBorder)
	}
}