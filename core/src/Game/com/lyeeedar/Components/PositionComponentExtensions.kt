package com.lyeeedar.Components

import com.lyeeedar.Board.Tile
import com.lyeeedar.Direction
import com.lyeeedar.Util.Point

var PositionComponent.tile: Tile?
	get() = position as? Tile
	set(value)
	{
		if (value != null) position = value
	}

val PositionComponent.tiles: Sequence<Tile>
	get()
	{
		val tile = tile!!
		return sequence {
			for (x in 0 until size)
			{
				for (y in 0 until size)
				{
					val t = tile.grid.getTile(tile, x, y) ?: continue
					yield(t)
				}
			}
		}
	}

fun PositionComponent.isOnTile(point: Point): Boolean
{
	return tiles.contains(point)
}

fun PositionComponent.getEdgeTiles(dir: Direction): com.badlogic.gdx.utils.Array<Tile>
{
	val tile = position as? Tile
			   ?: throw Exception("Position must be a tile!")

	var xstep = 0
	var ystep = 0

	var sx = 0
	var sy = 0

	if ( dir == Direction.NORTH )
	{
		sx = 0
		sy = size - 1

		xstep = 1
		ystep = 0
	}
	else if ( dir == Direction.SOUTH )
	{
		sx = 0
		sy = 0

		xstep = 1
		ystep = 0
	}
	else if ( dir == Direction.EAST )
	{
		sx = size - 1
		sy = 0

		xstep = 0
		ystep = 1
	}
	else if ( dir == Direction.WEST )
	{
		sx = 0
		sy = 0

		xstep = 0
		ystep = 1
	}

	val tiles = com.badlogic.gdx.utils.Array<Tile>(1)
	for (i in 0 until size)
	{
		val t = tile.grid.getTile(tile, sx + xstep * i, sy + ystep * i) ?: continue
		tiles.add(t)
	}

	return tiles
}

fun PositionComponent.isValidTile(t: Tile, entity: Entity): Boolean
{
	for (x in 0 until size)
	{
		for (y in 0 until size)
		{
			val tile = t.grid.getTile(t, x, y)
			if (tile == null || !tile.canHaveOrb ||
				(tile.contents != null && tile.contents != entity) ||
				(tile.contents != null && !tile.contents!!.isBasicOrb()))
			{
				return false
			}
		}
	}

	return true
}

fun PositionComponent.removeFromTile(entity: Entity)
{
	if (tile == null) return

	for (x in 0 until size)
	{
		for (y in 0 until size)
		{
			val tile = tile!!.grid.getTile(tile!!, x, y) ?: continue
			if (tile.contents == entity) tile.contents = null
		}
	}
}

fun PositionComponent.addToTile(entity: Entity, delay: Float = 0f)
{
	val t = tile!!
	for (x in 0 until size)
	{
		for (y in 0 until size)
		{
			val tile = t.grid.getTile(t, x, y) ?: continue

			val existing = tile.contents
			if (existing != null && existing != entity)
			{
				val matchable = existing.matchable()
				if (matchable != null)
				{
					val sprite = matchable.desc.death.copy()
					sprite.renderDelay = delay

					val renderable = existing.renderable()
					if (renderable != null)
					{
						sprite.colour = renderable.renderable.colour
					}

					tile.effects.add(sprite)
				}
			}

			tile.contents = entity
		}
	}
}

fun PositionComponent.setTile(entity: Entity, tile: Tile)
{
	this.tile = tile
	addToTile(entity)
}

fun PositionComponent.doMove(t: Tile, entity: Entity)
{
	removeFromTile(entity)

	position = t

	addToTile(entity)
}
