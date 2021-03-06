package com.lyeeedar.Board

import com.badlogic.gdx.math.Vector2
import com.lyeeedar.Components.*
import com.lyeeedar.Game.Buff
import com.lyeeedar.Game.BuffData
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
import com.lyeeedar.Util.AssetManager
import kotlin.math.min
import ktx.math.minus

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
	if (entity.matchable() == null)
	{
		throw RuntimeException("Entity does not have matchable!")
	}

	entity.addComponent(ComponentType.MonsterEffect)
	entity.monsterEffect()!!.set(monsterEffect)

	entity.addComponent(ComponentType.Tutorial)
	val tutorialComponent = entity.tutorial()!!
	tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
		if (!Statics.settings.get("Attack", false) )
		{
			val tutorial = Tutorial("Attack")
			tutorial.addPopup(Localisation.getText("monstereffect.tutorial.1", "UI"), gridWidget.getRect(entity))
			tutorial.addPopup(Localisation.getText("monstereffect.tutorial.2", "UI"), gridWidget.getRect(entity))
			return tutorial
		}

		return null
	}

	return entity
}

class MonsterEffectData : XmlDataClass()
{
	var damage: Int = 1
	var amount: Int = 1
	
	var monsterDesc: MonsterDesc? = null
	
	var faction: String? = null
	var name: String? = null
	var difficulty: Int = 0
	var isSummon: Boolean = false
	
	var spawnEffect: ParticleEffect? = null
	
	var debuff: BuffData? = null

	override fun load(xmlData: XmlData)
	{
	/* Autogenerated method contents. Do not modify. */
		damage = xmlData.getInt("Damage", 1)
		amount = xmlData.getInt("Amount", 1)
		val monsterDescEl = xmlData.getChildByName("MonsterDesc")
		if (monsterDescEl != null)
		{
			monsterDesc = MonsterDesc()
			monsterDesc!!.load(monsterDescEl)
		}
		faction = xmlData.get("Faction", null)
		name = xmlData.get("Name", null)
		difficulty = xmlData.getInt("Difficulty", 0)
		isSummon = xmlData.getBoolean("IsSummon", false)
		spawnEffect = AssetManager.tryLoadParticleEffect(xmlData.getChildByName("SpawnEffect"))?.getParticleEffect()
		val debuffEl = xmlData.getChildByName("Debuff")
		if (debuffEl != null)
		{
			debuff = BuffData()
			debuff!!.load(debuffEl)
		}
	}
}

class MonsterEffect(val effect: MonsterEffectType, val data: MonsterEffectData)
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
		val contents = tile.contents ?: return

		when(effect)
		{
			MonsterEffectType.ATTACK -> applyAttack(grid, contents)
			MonsterEffectType.BIGATTACK -> applyBigAttack(grid, contents)
			MonsterEffectType.HEAL -> applyHeal(grid, tile)
			MonsterEffectType.SUMMON -> applySummon(grid, tile)
			MonsterEffectType.DEBUFF -> applyDebuff(grid, tile)
			else -> throw Exception("Unhandled monster effect type '$effect'!")
		}

		grid.replay.logAction("Activating monster effect $effect at (${tile.toShortString()})")
	}

	fun applyAttack(grid: Grid, entity: Entity)
	{
		grid.onAttacked(entity)
	}

	fun applyBigAttack(grid: Grid, entity: Entity)
	{
		val dam = data.damage

		for (i in 0 until dam)
		{
			grid.onAttacked(entity)
		}
	}

	fun applyHeal(grid: Grid, srcTile: Tile)
	{
		val heal = data.amount

		for (tile in grid.monsterTiles)
		{
			val entity = tile.contents
			val monster = entity?.damageable() ?: continue
			monster.hp += heal
			monster.hp = min(monster.hp, monster.maxhp.toFloat())

			val monsterTile = entity.pos()!!.tile!!

			val diff = monsterTile.getPosDiff(srcTile)
			diff[0].y *= -1
			entity.renderable()!!.renderable.animation = BumpAnimation.obtain().set(0.2f, diff)

			val dst = entity.pos()!!.tile!!.euclideanDist(srcTile)
			val animDuration = 0.4f + dst * 0.025f
			val attackSprite = actualSprite.copy()
			attackSprite.drawActualSize = false
			attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
			attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			monsterTile.effects.add(attackSprite)

			val sprite = AssetManager.loadSprite("EffectSprites/Heal/Heal", 0.1f, Colour(0f,1f,0f,1f))
			sprite.size[0] = entity.pos()!!.size
			sprite.size[1] = entity.pos()!!.size
			sprite.renderDelay = animDuration

			monsterTile.effects.add(sprite)
		}
	}

	fun applySummon(grid: Grid, tile: Tile)
	{
		var desc = data.monsterDesc
		if (desc == null)
		{
			val factionName = data.faction

			val faction: Faction
			if (!factionName.isNullOrBlank())
			{
				val factionPath = XmlData.enumeratePaths("Factions", "Faction").first { it.endsWith("$factionName.xml") }.split("Factions/")[1]

				faction = Faction.load(factionPath)
			}
			else
			{
				faction = grid.level.chosenFaction!!
			}

			val name = data.name ?: ""
			desc = if (name.isBlank()) faction.get(1, grid) else (faction.get(name) ?: faction.get(1, grid))!!
		}

		val difficulty = data.difficulty

		val summoned = desc.getEntity(difficulty, data.isSummon, grid)

		summoned.pos()!!.tile = tile
		summoned.pos()!!.addToTile(summoned)

		val spawnEffect = data.spawnEffect
		if (spawnEffect != null)
		{
			val spawnEffect = spawnEffect.copy()
			tile.effects.add(spawnEffect)
		}
	}

	fun applyDebuff(grid: Grid, tile: Tile)
	{
		val buff = Buff(data.debuff!!)

		val dstTable = GridScreen.instance.debuffTable

		val sprite = actualSprite.copy()
		val dst = dstTable.localToStageCoordinates(Vector2(Random.random() * dstTable.width, Random.random() * dstTable.height))
		val moteDst = dst.cpy() - Vector2(GridWidget.instance.tileSize / 2f, GridWidget.instance.tileSize / 2f)
		val src = GridWidget.instance.pointToScreenspace(tile)

		spawnMote(src, moteDst, sprite, GridWidget.instance.tileSize, {}, animSpeed = 0.35f, leap = true)
		tile.addDelayedAction(
			{
				Global.player.leveldebuffs.add(buff)

				if (!Global.resolveInstantly)
				{
					GridScreen.instance.updateBuffTable()
				}
			}, 0.35f)

	}
}