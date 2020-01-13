package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Screens.GridScreen
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
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

fun addMonsterEffect(entity: Entity, monsterEffect: MonsterEffect): Entity
{
	entity.add(MonsterEffectComponent.obtain().set(monsterEffect))

	val tutorialComponent = TutorialComponent.obtain()
	tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
		if (!Statics.settings.get("Attack", false) )
		{
			val tutorial = Tutorial("Attack")
			tutorial.addPopup("This is an attack. The pips surrounding the skull indicate the turns remaining until it activates.", gridWidget.getRect(entity))
			tutorial.addPopup("Match it like a normal orb to remove it from the board. If you fail to remove it then you will lose 1 hp", gridWidget.getRect(entity))
			return tutorial
		}

		return null
	}
	entity.add(tutorialComponent)

	return entity
}

class MonsterEffect(val effect: MonsterEffectType, val data: ObjectMap<String, Any>)
{
	val actualSprite: Sprite = when (effect)
	{
		MonsterEffectType.ATTACK -> AssetManager.loadSprite("Oryx/Custom/items/skull_small", drawActualSize = true)
		MonsterEffectType.BIGATTACK -> AssetManager.loadSprite("Oryx/Custom/items/skull_large", drawActualSize = true)
		MonsterEffectType.HEAL -> AssetManager.loadSprite("Oryx/Custom/items/heart", drawActualSize = true)
		MonsterEffectType.SUMMON -> AssetManager.loadSprite("Oryx/Custom/items/egg", drawActualSize = true)
		MonsterEffectType.DEBUFF -> AssetManager.loadSprite("Oryx/Custom/items/Debuff", drawActualSize = true)
		else -> throw Exception("Unhandled monster effect type '$effect'!")
	}

	var timer: Int = -1
	var delayDisplay: Float = 0f
	var addedSprite = false

	fun apply(grid: Grid, tile: Tile)
	{
		when(effect)
		{
			MonsterEffectType.ATTACK -> applyAttack(grid, tile.contents!!)
			MonsterEffectType.BIGATTACK -> applyBigAttack(grid, tile.contents!!)
			MonsterEffectType.HEAL -> applyHeal(grid, tile)
			MonsterEffectType.SUMMON -> applySummon(grid, tile)
			MonsterEffectType.DEBUFF -> applyDebuff(grid, tile)
			else -> throw Exception("Unhandled monster effect type '$effect'!")
		}
	}

	fun applyAttack(grid: Grid, entity: Entity)
	{
		grid.onAttacked(entity)
	}

	fun applyBigAttack(grid: Grid, entity: Entity)
	{
		val dam = data["DAMAGE"].toString().toInt()

		for (i in 0 until dam)
		{
			grid.onAttacked(entity)
		}
	}

	fun applyHeal(grid: Grid, srcTile: Tile)
	{
		val heal = data["AMOUNT", "1"].toString().toFloat()

		for (tile in grid.monsterTiles)
		{
			val entity = tile.contents
			val monster = entity?.damageable() ?: continue
			monster.hp += heal
			monster.hp = min(monster.hp, monster.maxhp.toFloat())

			val monsterTile = entity.pos().tile!!

			val diff = monsterTile.getPosDiff(srcTile)
			diff[0].y *= -1
			entity.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, diff)

			val dst = entity.pos().tile!!.euclideanDist(srcTile)
			val animDuration = 0.4f + dst * 0.025f
			val attackSprite = actualSprite.copy()
			attackSprite.drawActualSize = false
			attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
			attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			monsterTile.effects.add(attackSprite)

			val sprite = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.1f, Colour(0f,1f,0f,1f))
			sprite.size[0] = entity.pos().size
			sprite.size[1] = entity.pos().size
			sprite.renderDelay = animDuration

			monsterTile.effects.add(sprite)
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
			desc = if (name.isBlank()) faction.get(1) else (faction.get(name) ?: faction.get(1))!!
		}

		val difficulty = data["DIFFICULTY", "0"].toString().toInt()

		val summoned = desc.getEntity(difficulty, data["ISSUMMON", "false"].toString().toBoolean())

		summoned.pos().tile = tile
		summoned.pos().addToTile(summoned)

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