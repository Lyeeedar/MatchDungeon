package com.lyeeedar.Board

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Game.Buff
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.XmlData
import ktx.math.minus
import kotlin.math.min

enum class MonsterEffectType
{
	ATTACK,
	BIGATTACK,
	HEAL,
	SUMMON,
	DEBUFF
}

class MonsterEffect(val effect: MonsterEffectType, val data: ObjectMap<String, Any>, desc: OrbDesc, theme: Theme) : Matchable(theme)
{
	override var desc: OrbDesc = OrbDesc()
		set(value)
		{
			field = value
			sprite.colour = desc.sprite.colour
		}
	val actualSprite: Sprite

	override val canMatch: Boolean
		get() = true

	override var markedForDeletion: Boolean = false
	override var deletionEffectDelay: Float = 0f

	override val canMove: Boolean
		get() = !sealed

	var timer: Int = -1
	var delayDisplay: Float = 0f

	init
	{
		actualSprite = when (effect)
		{
			MonsterEffectType.ATTACK -> AssetManager.loadSprite("Oryx/Custom/items/skull_small", drawActualSize = true)
			MonsterEffectType.BIGATTACK -> AssetManager.loadSprite("Oryx/Custom/items/skull_large", drawActualSize = true)
			MonsterEffectType.HEAL -> AssetManager.loadSprite("Oryx/Custom/items/heart", drawActualSize = true)
			MonsterEffectType.SUMMON -> AssetManager.loadSprite("Oryx/Custom/items/egg", drawActualSize = true)
			MonsterEffectType.DEBUFF -> AssetManager.loadSprite("Oryx/Custom/items/Debuff", drawActualSize = true)
			else -> throw Exception("Unhandled monster effect type '$effect'!")
		}
		sprite = desc.sprite.copy()

		this.desc = desc
	}

	fun apply(grid: Grid, tile: Tile)
	{
		when(effect)
		{
			MonsterEffectType.ATTACK -> applyAttack(grid)
			MonsterEffectType.BIGATTACK -> applyBigAttack(grid)
			MonsterEffectType.HEAL -> applyHeal(grid, tile)
			MonsterEffectType.SUMMON -> applySummon(grid, tile)
			MonsterEffectType.DEBUFF -> applyDebuff(grid, tile)
			else -> throw Exception("Unhandled monster effect type '$effect'!")
		}
	}

	fun applyAttack(grid: Grid)
	{
		grid.onAttacked(this)
	}

	fun applyBigAttack(grid: Grid)
	{
		val dam = data["DAMAGE"].toString().toInt()

		for (i in 0 until dam)
		{
			grid.onAttacked(this)
		}
	}

	fun applyHeal(grid: Grid, srcTile: Tile)
	{
		val heal = data["AMOUNT", "1"].toString().toFloat()

		for (tile in grid.grid)
		{
			val monster = tile.monster ?: continue
			monster.hp += heal
			monster.hp = min(monster.hp, monster.maxhp.toFloat())

			val diff = monster.tiles[0, 0].getPosDiff(srcTile)
			diff[0].y *= -1
			monster.sprite.animation = BumpAnimation.obtain().set(0.2f, diff)

			val dst = monster.tiles[0, 0].euclideanDist(srcTile)
			val animDuration = 0.4f + dst * 0.025f
			val attackSprite = actualSprite.copy()
			attackSprite.drawActualSize = false
			attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
			attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			monster.tiles[0, 0].effects.add(attackSprite)

			val sprite = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.1f, Colour(0f,1f,0f,1f))
			sprite.size[0] = monster.size
			sprite.size[1] = monster.size
			sprite.renderDelay = animDuration

			monster.tiles[0, 0].effects.add(sprite)
		}
	}

	fun applySummon(grid: Grid, tile: Tile)
	{
		var desc = data["MONSTERDESC", null] as? MonsterDesc
		if (desc == null)
		{
			val factionName = data["FACTION", null]?.toString()?.toUpperCase()

			val faction: Faction
			if (!factionName.isNullOrBlank())
			{
				val factionPath = XmlData.enumeratePaths("Factions", "Faction").first { it.toUpperCase().endsWith("$factionName.XML") }.split("Factions/")[1]

				faction = Faction.load(factionPath)
			}
			else
			{
				faction = grid.level.chosenFaction!!
			}

			val name = data["NAME", null]?.toString() ?: ""
			desc = if (name.isBlank()) faction.get(1) else faction.get(name)
		}

		val difficulty = data["DIFFICULTY", "0"].toString().toInt()

		val summoned = Monster(desc!!, difficulty)
		summoned.isSummon = data["ISSUMMON", "false"].toString().toBoolean()

		summoned.setTile(tile, grid)

		val spawnEffect = data["SPAWNEFFECT", null] as? ParticleEffect
		if (spawnEffect != null)
		{
			val spawnEffect = spawnEffect.copy()
			tile.effects.add(spawnEffect)
		}
	}

	fun applyDebuff(grid: Grid, tile: Tile)
	{
		val buff = data["DEBUFF"] as Buff

		val dstTable = GridScreen.instance.debuffTable

		val sprite = actualSprite.copy()
		val dst = dstTable.localToStageCoordinates(Vector2(Random.random() * dstTable.width, Random.random() * dstTable.height))
		val moteDst = dst.cpy() - Vector2(GridWidget.instance.tileSize / 2f, GridWidget.instance.tileSize / 2f)
		val src = GridWidget.instance.pointToScreenspace(tile)

		Mote(src, moteDst, sprite, GridWidget.instance.tileSize,
			 {
				 Global.player.leveldebuffs.add(buff)
				 GridScreen.instance.updateBuffTable()
			 }, animSpeed = 0.35f, leap = true)

	}
}