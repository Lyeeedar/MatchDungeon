package com.lyeeedar.Board.GridUpdate

import com.lyeeedar.Board.Grid
import com.lyeeedar.Components.damageable
import com.lyeeedar.Components.pos
import com.lyeeedar.Components.tile
import com.lyeeedar.Game.Global
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.Systems.GridSystem
import ktx.collections.toGdxArray

class OnTurnCleanupUpdateStep : AbstractUpdateStep()
{
	override fun doUpdate(gridSystem: GridSystem): Boolean
	{
		gridSystem.animSpeedMultiplier = 1f

		return true
	}

	override fun doTurn(gridSystem: GridSystem)
	{
		val grid = gridSystem.grid!!

		for (tile in grid.grid)
		{
			if (tile.contents != null)
			{
				val damageableComponent = tile.contents!!.damageable()
				val positionComponent = tile.contents!!.pos()

				if (damageableComponent != null && positionComponent.tile == tile)
				{
					damageableComponent.damSources.clear()
					damageableComponent.remainingReduction = damageableComponent.damageReduction
				}
			}
		}

		for (buff in Global.player.levelbuffs.toGdxArray())
		{
			buff.remainingDuration--
			if (buff.remainingDuration <= 0)
			{
				Global.player.levelbuffs.removeValue(buff, true)
			}
		}

		for (debuff in Global.player.leveldebuffs.toGdxArray())
		{
			debuff.remainingDuration--
			if (debuff.remainingDuration <= 0)
			{
				Global.player.leveldebuffs.removeValue(debuff, true)
			}
		}

		GridScreen.instance.updateBuffTable()

		grid.gainedBonusPower = false
		grid.poppedSpreaders.clear()
		grid.noMatchTimer = 0f

		gridSystem.animSpeedMultiplier = 1f
		gridSystem.inTurn = false
		gridSystem.matchCount = 0
	}
}