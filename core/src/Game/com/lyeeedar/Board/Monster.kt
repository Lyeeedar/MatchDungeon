package com.lyeeedar.Board

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Components.*
import com.lyeeedar.Direction
import com.lyeeedar.Game.Ability.Permuter
import com.lyeeedar.Game.Ability.Targetter
import com.lyeeedar.Game.Global
import com.lyeeedar.Renderables.Animation.BumpAnimation
import com.lyeeedar.Renderables.Animation.ExpandAnimation
import com.lyeeedar.Renderables.Animation.LeapAnimation
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Statistic
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.*
import ktx.collections.addAll
import ktx.collections.set
import ktx.collections.toGdxArray

fun Entity.isMonster(): Boolean
{
	if (this.hasComponent(DamageableComponent::class.java) && this.ai()?.ai is MonsterAI)
	{
		return true
	}

	return false
}

class MonsterDesc
{
	lateinit var name: String
	lateinit var sprite: Sprite
	lateinit var death: ParticleEffect
	var attackNumPips: Int = 5
	var attackCooldown: Point = Point(6, 6)
	var attackDamage: Int = 1
	var size: Int = 1
	var hp: Int = 10
	var damageReduction: Int = 0
	val abilities = Array<AbstractMonsterAbility>()
	val stages = Array<MonsterDesc>()

	var originalDesc: MonsterDesc? = null

	fun getEntity(difficulty: Int, isSummon: Boolean, grid: Grid): Entity
	{
		val archetype = EntityArchetypeComponent.obtain().set(EntityArchetype.MONSTER)

		val position = PositionComponent.obtain()
		position.size = size

		val renderable = RenderableComponent.obtain().set(sprite.copy())

		val damageable = DamageableComponent.obtain()
		damageable.deathEffect = death.copy()
		damageable.maxhp = hp + (hp.toFloat() * (difficulty.toFloat() / 7f)).ciel()
		damageable.isSummon = isSummon
		damageable.damageReduction = damageReduction

		if (difficulty >= 3)
		{
			damageable.damageReduction += (difficulty.toFloat() / 5f).ciel()
		}

		val ai = AIComponent.obtain()
		ai.ai = MonsterAI(this, difficulty, grid)

		val tutorialComponent = TutorialComponent.obtain()
		tutorialComponent.displayTutorial = fun (grid, entity, gridWidget): Tutorial? {
			if (damageable.damageReduction > 0  && !Statics.settings.get("DR", false))
			{
				val tutorial = Tutorial("DR")
				tutorial.addPopup("This enemy has damage resistance, represented by the grey pips on its health bar. At the end of each turn it will replenish those pips, so focus on those big hits!", gridWidget.getRect(entity))
				return tutorial
			}

			if (!Statics.settings.get("Monster", false) )
			{
				val tutorial = Tutorial("Monster")
				tutorial.addPopup("This is an enemy. The red bar beneath it is its remaining health. Match in the tiles surrounding it to damage it.", gridWidget.getRect(entity))
				return tutorial
			}

			if ((ai.ai as MonsterAI).desc.stages.size > 0 && !Statics.settings.get("MonsterStages", false))
			{
				val tutorial = Tutorial("MonsterStages")
				tutorial.addPopup("This enemy has multiple stages, indicated by the orbs above its hp bar. When its hp bar is empty it will mutate into a new creature, so watch out!", gridWidget.getRect(entity))
				return tutorial
			}

			return null
		}

		val entity = EntityPool.obtain()
		entity.add(archetype)
		entity.add(position)
		entity.add(renderable)
		entity.add(damageable)
		entity.add(ai)
		entity.add(tutorialComponent)

		return entity
	}

	companion object
	{
		fun load(xml: XmlData): MonsterDesc
		{
			val desc = MonsterDesc()

			desc.name = xml.get("Name", xml.toString())!!

			desc.sprite = AssetManager.loadSprite(xml.getChildByName("Sprite")!!)
			desc.death = AssetManager.loadParticleEffect(xml.getChildByName("Death")!!).getParticleEffect()

			desc.attackNumPips = xml.getInt("AttackNumPips")

			val atkCooldown = xml.get("AttackCooldown").split(',')
			desc.attackCooldown = Point(atkCooldown[0].toInt(), atkCooldown[1].toInt())

			desc.attackDamage = xml.getInt("AttackDamage", 1)

			desc.size = xml.getInt("Size", 1)

			desc.hp = xml.getInt("HP", 10)

			desc.damageReduction = xml.getInt("DamageReduction", 0)

			val abilitiesEl = xml.getChildByName("Abilities")
			if (abilitiesEl != null)
			{
				for (i in 0 until abilitiesEl.childCount)
				{
					val el = abilitiesEl.getChild(i)
					val ability = AbstractMonsterAbility.load(el)
					desc.abilities.add(ability)
				}
			}

			val stagesEl = xml.getChildByName("Stages")
			if (stagesEl != null)
			{
				for (stageEl in stagesEl.children)
				{
					val monsterDesc = load(stageEl)
					monsterDesc.name = desc.name
					desc.stages.add(monsterDesc)
					monsterDesc.originalDesc = desc
				}
			}

			return desc
		}
	}
}

class MonsterAI(val desc: MonsterDesc, val difficulty: Int, grid: Grid) : AbstractGridAI()
{
	val abilities = Array<AbstractMonsterAbility>()

	var fastAttacks = -1
	var powerfulAttacks = -1

	var atkCooldown = 0
	var attackDamage = 1
	var attackNumPips = 7
	var attackCooldown: Point = Point(6, 6)

	init
	{
		attackDamage = desc.attackDamage
		attackNumPips = desc.attackNumPips
		attackCooldown = desc.attackCooldown.copy()

		abilities.addAll(desc.abilities.map{ it.copy(grid) }.toGdxArray())

		if (difficulty >= 1)
		{
			atkCooldown -= (difficulty.toFloat() / 3f).ciel()

			for (ability in abilities)
			{
				ability.cooldownMin -= (difficulty.toFloat() / 4f).ciel()
				ability.cooldownMax -= (difficulty.toFloat() / 4f).ciel()
			}
		}

		if (difficulty >= 2)
		{
			if (attackNumPips > 4)
			{
				attackNumPips -= (difficulty.toFloat() / 5f).ciel()
				if (attackNumPips < 4)
				{
					attackNumPips = 4
				}
			}
		}

		if (difficulty >= 3)
		{
			attackDamage += (difficulty.toFloat() / 4f).ciel()
		}

		if (desc.attackNumPips > 0)
		{
			var max = desc.attackCooldown.max
			max += (Global.player.getStat(Statistic.HASTE) * max).toInt()

			atkCooldown = (grid.ran.nextFloat() * max).toInt()
		}
		else
		{
			atkCooldown = Int.MAX_VALUE
		}
	}

	override fun onTurn(entity: Entity, grid: Grid)
	{
		if (fastAttacks > 0)
		{
			fastAttacks--

			atkCooldown = 0
		}

		if (powerfulAttacks > 0)
		{
			powerfulAttacks--
		}

		atkCooldown--
		if (atkCooldown <= 0)
		{
			var min = attackCooldown.min
			min += (Global.player.getStat(Statistic.HASTE) * min).toInt()

			var max = attackCooldown.max
			max += (Global.player.getStat(Statistic.HASTE) * max).toInt()

			atkCooldown = min + grid.ran.nextInt(max - min)

			// do attack
			val tile = grid.basicOrbTiles.filter { validAttack(grid, it) }.randomOrNull(grid.ran)

			if (tile?.contents != null)
			{
				val startTile = entity.pos().tile!!

				val damage = if (powerfulAttacks > 0) attackDamage + 2 else attackDamage

				val monsterEffectType = if (damage > 1) MonsterEffectType.BIGATTACK else MonsterEffectType.ATTACK
				val data = ObjectMap<String, Any>()
				data["DAMAGE"] = damage.toString()

				val monsterEffect = MonsterEffect(monsterEffectType, data)
				monsterEffect.timer = attackNumPips + (Global.player.getStat(Statistic.HASTE) * attackNumPips).toInt()

				addMonsterEffect(tile.contents!!, monsterEffect)

				grid.replay.logAction("Monster ${entity.niceName()} attacking (${tile.toShortString()})")

				if (!Global.resolveInstantly)
				{
					val diff = tile.getPosDiff(startTile)
					diff[0].y *= -1
					entity.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, diff)

					val dst = tile.euclideanDist(startTile)
					val animDuration = 0.4f + tile.euclideanDist(startTile) * 0.025f
					val attackSprite = monsterEffect.actualSprite.copy()
					attackSprite.colour = tile.contents!!.renderable().renderable.colour
					attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
					attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
					tile.effects.add(attackSprite)

					monsterEffect.delayDisplay = animDuration
				}
			}
		}

		for (ability in abilities)
		{
			ability.cooldownTimer--
			if (ability.cooldownTimer <= 0)
			{
				var min = ability.cooldownMin
				min += (Global.player.getStat(Statistic.HASTE) * min).toInt()

				var max = ability.cooldownMax
				max += (Global.player.getStat(Statistic.HASTE) * max).toInt()

				ability.cooldownTimer = min + grid.ran.nextInt(max - min)
				ability.activate(entity, grid)
			}
		}
	}
}

fun validAttack(grid: Grid, tile: Tile): Boolean
{
	if (tile.spreader != null) return false
	if (tile.contents?.matchable() != null) return false

	// dont allow attacks in choke points
	for (dir in Direction.CardinalValues)
	{
		val ntile = grid.tile(tile + dir) ?: return false
		if (!ntile.canHaveOrb) return false
	}

	return true
}

abstract class AbstractMonsterAbility
{
	var cooldownTimer: Int = 0
	var cooldownMin: Int = 1
	var cooldownMax: Int = 1
	lateinit var targetRestriction: Targetter
	var targetCount: Int = 1
	lateinit var permuter: Permuter
	var usages = -1

	val data = ObjectMap<String, Any>()

	fun copy(grid: Grid): AbstractMonsterAbility
	{
		val ability = doCopy()
		ability.cooldownMin = cooldownMin
		ability.cooldownMax = cooldownMax
		ability.cooldownTimer = cooldownMin + grid.ran.nextInt(cooldownMax - cooldownMin)
		ability.usages = usages
		ability.targetRestriction = targetRestriction
		ability.targetCount = targetCount
		ability.permuter = permuter
		ability.data.putAll(data)

		return ability
	}

	abstract fun doCopy(): AbstractMonsterAbility

	fun activate(entity: Entity, grid: Grid)
	{
		if (usages == 0)
		{
			return
		}

		if (usages != -1)
		{
			usages--
		}

		if (!Statics.release && !Global.resolveInstantly)
		{
			println("Monster trying to use ability '${this.javaClass.name.split(".Monster")[1].replace("Ability", "")}'")
		}

		val monsterTile = entity.pos().tile!!

		val availableTargets = Array<Tile>()

		val range = data["RANGE", "1"].toString().toInt()
		val minRange = data["MINRANGE", "0"].toString().toInt()

		availableTargets.addAll(grid.basicOrbTiles.filter { it.taxiDist(monsterTile) in minRange..range })

		var validTargets = availableTargets.filter { targetRestriction.isValid(it, data) }

		if (targetRestriction.type == Targetter.Type.ORB)
		{
			validTargets = validTargets.filter { validAttack(grid, it) }
		}

		val chosen = validTargets.asSequence().random(targetCount, grid.ran).toList().toGdxArray()

		val finalTargets = Array<Tile>()

		for (target in chosen)
		{
			val source = entity.pos().tile!!
			for (t in permuter.permute(target, grid, data, chosen, null, source))
			{
				if (!finalTargets.contains(t, true))
				{
					finalTargets.add(t)
				}
			}
		}

		val coverage = data["COVERAGE", "1"]?.toString()?.toFloat() ?: 1f
		if (coverage < 1f)
		{
			val chosenCount = (finalTargets.size.toFloat() * coverage).ciel()
			while (finalTargets.size > chosenCount)
			{
				finalTargets.removeRandom(grid.ran)
			}
		}

		if (finalTargets.size > 0 && !Global.resolveInstantly)
		{
			val diff = finalTargets[0].getPosDiff(entity.pos().tile!!)
			diff[0].y *= -1
			entity.renderable().renderable.animation = BumpAnimation.obtain().set(0.2f, diff)
		}

		grid.replay.logAction("Activating monster ability $this on targets " + finalTargets.joinToString(" ") { "(${it.toShortString()})" })

		activate(entity, grid, finalTargets)
	}

	abstract fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)

	abstract fun parse(xml: XmlData)

	companion object
	{
		fun load(xml: XmlData) : AbstractMonsterAbility
		{
			val ability = when (xml.get("Effect", "Attack")!!.toUpperCase())
			{
				"MOVE", "DASH" -> MonsterMoveAbility()
				"ATTACK", "SEALEDATTACK", "HEAL", "DELAYEDSUMMON", "DEBUFF" -> MonsterMonsterEffectAbility()
				"SEAL" -> MonsterSealAbility()
				"SUMMON" -> MonsterSummonAbility()
				"SPREADER" -> MonsterSpreaderAbility()
				"SELFBUFF" -> MonsterSelfBuffAbility()
				"BLOCK" -> MonsterBlockAbility()
				else -> throw RuntimeException("Unknown monster ability type '" + xml.get("Effect") + "'")
			}
			ability.parse(xml)

			val cooldown = xml.get("Cooldown").split(",")
			ability.cooldownMin = cooldown[0].toInt()
			ability.cooldownMax = cooldown[1].toInt()
			ability.cooldownTimer = ability.cooldownMin + (ability.cooldownMax - ability.cooldownMin) / 2

			ability.usages = xml.getInt("Usages", -1)

			ability.targetCount = xml.getInt("Count", 1)

			ability.targetRestriction = Targetter(Targetter.Type.valueOf(xml.get("TargetRestriction", "Orb")!!.toUpperCase()))
			ability.permuter = Permuter(Permuter.Type.valueOf(xml.get("Permuter", "Single")!!.toUpperCase()))

			val dEl = xml.getChildByName("Data")
			if (dEl != null)
			{
				val dBlock = loadDataBlock(dEl)
				ability.data.putAll(dBlock)
			}

			return ability
		}
	}
}

class MonsterMoveAbility : AbstractMonsterAbility()
{
	enum class MoveType
	{
		BASIC,
		LEAP,
		TELEPORT
	}

	var isDash: Boolean = false

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		val target = targets.filter{ entity.pos().isValidTile(it, entity) }.asSequence().random(grid.ran)

		entity.renderable().renderable.animation = null

		if (target != null)
		{
			val monsterSrc = entity.pos().tile!!

			val dst = monsterSrc.euclideanDist(target)
			var animDuration = 0.25f + dst * 0.025f

			entity.pos().removeFromTile(entity)
			entity.pos().tile = target
			entity.pos().addToTile(entity, animDuration - 0.1f)

			val diff = target.getPosDiff(monsterSrc)
			diff[0].y *= -1

			val moveType: MoveType
			if (isDash)
			{
				moveType = MoveType.BASIC
			}
			else if (data.containsKey("MOVETYPE"))
			{
				val moveTypeStr = data["MOVETYPE"].toString().toUpperCase()
				moveType = when(moveTypeStr)
				{
					"BASIC" -> MoveType.BASIC
					"LEAP" -> MoveType.LEAP
					"TELEPORT" -> MoveType.TELEPORT
					else -> throw Exception("Unknown move type '$moveTypeStr'!")
				}
			}
			else
			{
				moveType = MoveType.BASIC
			}

			if (moveType == MoveType.LEAP)
			{
				entity.renderable().renderable.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				entity.renderable().renderable.animation = ExpandAnimation.obtain().set(animDuration, 1f, 2f, false)
			}
			else if (moveType == MoveType.TELEPORT)
			{
				animDuration = 0.2f
				entity.renderable().renderable.renderDelay = animDuration
				entity.renderable().renderable.showBeforeRender = false
			}
			else
			{
				entity.renderable().renderable.animation = MoveAnimation.obtain().set(animDuration, UnsmoothedPath(diff), Interpolation.linear)
			}

			val startParticle = data["STARTEFFECT", null]
			val endParticle = data["ENDEFFECT", null]

			if (startParticle is ParticleEffect)
			{
				val particle = startParticle.copy()
				particle.size[0] = entity.pos().size
				particle.size[1] = entity.pos().size

				monsterSrc.effects.add(particle)
			}

			if (endParticle is ParticleEffect)
			{
				val particle = endParticle.copy()
				particle.size[0] = entity.pos().size
				particle.size[1] = entity.pos().size

				particle.renderDelay = animDuration

				target.effects.add(particle)
			}

			if (isDash)
			{
				// get line from start to end
				val points = monsterSrc.lineTo(target).toGdxArray()


				val maxDist = monsterSrc.euclideanDist(target)

				val hitEffect = data["HITEFFECT", null]

				var timer = data["NUMPIPS", 8].toString().toInt()
				timer += (Global.player.getStat(Statistic.HASTE) * timer).toInt()

				// make them all attacks in order
				for (point in points)
				{
					val tile = grid.tile(point)
					if (tile != null && validAttack(grid, tile))
					{
						val dist = monsterSrc.euclideanDist(point)
						val alpha = dist / maxDist
						val delay = animDuration * alpha

						tile.addDelayedAction(
							{
								addMonsterEffect(tile.contents!!, MonsterEffect(MonsterEffectType.ATTACK, ObjectMap()))

								if (hitEffect is ParticleEffect)
								{
									val particle = hitEffect.copy()
									tile.effects.add(particle)
								}

							}, delay)
					}
				}
			}
		}
	}

	override fun parse(xml: XmlData)
	{
		if (xml.get("Effect") == "DASH")
		{
			isDash = true
		}
	}

	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterMoveAbility()
		ability.isDash = isDash

		return ability
	}
}

class MonsterMonsterEffectAbility : AbstractMonsterAbility()
{
	enum class EffectType
	{
		ATTACK,
		SEALEDATTACK,
		HEAL,
		DELAYEDSUMMON,
		DEBUFF
	}

	lateinit var effect: EffectType

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		val monsterSrc = entity.pos().tile!!

		for (tile in targets)
		{
			val contents = tile.contents ?: continue
			if (!contents.isBasicOrb()) continue

			var speed = data.get("NUMPIPS", 8).toString().toInt()
			speed += (Global.player.getStat(Statistic.HASTE) * speed).toInt()

			val monsterEffectType: MonsterEffectType
			if (effect == EffectType.HEAL)
			{
				monsterEffectType = MonsterEffectType.HEAL
			}
			else if (effect == EffectType.DEBUFF)
			{
				monsterEffectType = MonsterEffectType.DEBUFF
			}
			else if (effect == EffectType.DELAYEDSUMMON)
			{
				monsterEffectType = MonsterEffectType.SUMMON
			}
			else
			{
				val dam = data.get("DAMAGE", "1").toString().toInt()
				monsterEffectType = if (dam > 1) MonsterEffectType.BIGATTACK else MonsterEffectType.ATTACK
			}

			val monsterEffect = MonsterEffect(monsterEffectType, data)
			addMonsterEffect(contents, monsterEffect)

			monsterEffect.timer = speed
			val diff = tile.getPosDiff(monsterSrc)
			diff[0].y *= -1

			val dst = tile.euclideanDist(monsterSrc)
			var animDuration = dst * 0.025f

			if (data["SHOWATTACKLEAP", "true"].toString().toBoolean())
			{
				animDuration += 0.4f
				val attackSprite = monsterEffect.actualSprite.copy()
				attackSprite.colour = contents.renderable().renderable.colour
				attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
				attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
				tile.effects.add(attackSprite)

				monsterEffect.delayDisplay = animDuration
			}
			else
			{
				val flightEffect = data["FLIGHTEFFECT", null]
				if (flightEffect is ParticleEffect)
				{
					animDuration += 0.4f
					val particle = flightEffect.copy()
					particle.animation = MoveAnimation.obtain().set(animDuration, diff)
					particle.killOnAnimComplete = true

					particle.rotation = getRotation(monsterSrc, tile)

					tile.effects.add(particle)

					monsterEffect.delayDisplay = animDuration
				}
			}

			val hitEffect = data["HITEFFECT", null]
			if (hitEffect is ParticleEffect)
			{
				val particle = hitEffect.copy()
				particle.renderDelay = animDuration

				animDuration += particle.lifetime / 2f
				monsterEffect.delayDisplay = animDuration

				tile.effects.add(particle)
			}

			if (effect == EffectType.SEALEDATTACK)
			{
				val strength = data.get("STRENGTH", "1").toString().toInt()
				val swappable = tile.contents?.swappable() ?: continue
				swappable.sealCount = strength
			}
		}
	}

	override fun parse(xml: XmlData)
	{
		effect = EffectType.valueOf(xml.get("Effect", "Attack")!!.toUpperCase())
	}

	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterMonsterEffectAbility()
		ability.effect = effect

		return ability
	}
}

class MonsterSealAbility : AbstractMonsterAbility()
{
	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterSealAbility()
		return ability
	}

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		val strength = data.get("STRENGTH", "1").toString().toInt()

		for (tile in targets)
		{
			val swappable = tile.contents?.swappable() ?: continue
			swappable.sealCount = strength
		}
	}

	override fun parse(xml: XmlData)
	{

	}

}

class MonsterBlockAbility : AbstractMonsterAbility()
{
	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterBlockAbility()
		return ability
	}

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		val strength = data.get("STRENGTH", "1").toString().toInt()
		val monsterSrc = entity.pos().tile!!

		for (tile in targets)
		{
			val block = createBlock(grid.level.theme, strength)

			val diff = tile.getPosDiff(monsterSrc)
			diff[0].y *= -1

			val dst = tile.euclideanDist(monsterSrc)
			val animDuration = 0.4f + dst * 0.025f
			val attackSprite = block.renderable().renderable.copy()
			attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
			attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			tile.effects.add(attackSprite)

			block.renderable().renderable.renderDelay = animDuration
			block.pos().tile = tile
			block.pos().addToTile(block, animDuration)
		}
	}

	override fun parse(xml: XmlData)
	{

	}

}

class MonsterSummonAbility : AbstractMonsterAbility()
{
	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterSummonAbility()
		return ability
	}

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		for (tile in targets)
		{
			var desc = data["MONSTERDESC", null] as? MonsterDesc
			if (desc == null)
			{
				val factionName = data["FACTION", null]?.toString()

				val faction: Faction
				if (!factionName.isNullOrBlank())
				{
					val factionPath = XmlData.enumeratePaths("Factions", "Faction")
						.first { it.toUpperCase().endsWith("${factionName.toUpperCase()}.XML") }
						.split("Factions/")[1]

					faction = Faction.load(factionPath)
				}
				else
				{
					faction = grid.level.chosenFaction!!
				}

				val name = data["NAME", null]?.toString() ?: ""
				desc = if (name.isBlank()) faction.get(1, grid) else (faction.get(name) ?: faction.get(1, grid))
			}

			val difficulty = data["DIFFICULTY", "0"].toString().toInt()

			val summoned = desc!!.getEntity(difficulty, data["ISSUMMON", "false"].toString().toBoolean(), grid)

			summoned.pos().tile = tile
			summoned.pos().addToTile(summoned)

			val spawnEffect = data["SPAWNEFFECT", null] as? ParticleEffect
			if (spawnEffect != null)
			{
				tile.effects.add(spawnEffect.copy())
			}
		}
	}

	override fun parse(xml: XmlData)
	{

	}

}

class MonsterSelfBuffAbility : AbstractMonsterAbility()
{
	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterSelfBuffAbility()
		return ability
	}

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		val type = data["BUFFTYPE", "Immunity"].toString()
		val duration =  data["DURATION", "1"].toString().toInt()

		val ai = entity.ai()!!.ai as MonsterAI

		when (type)
		{
			"Immunity" -> { entity.damageable()!!.immune = true; entity.damageable()!!.immuneCooldown = duration }
			"FastAttacks" -> ai.fastAttacks = duration
			"PowerfulAttacks" -> ai.powerfulAttacks = duration
			else -> throw Exception("Unknown monster selfbuff '$type'!")
		}

		val effect = data["PARTICLEEFFECT", null]
		if (effect is ParticleEffect)
		{
			val e = effect.copy()
			e.size[0] = entity.pos().size
			e.size[1] = entity.pos().size
			entity.pos().tile!!.effects.add(e)
		}
	}

	override fun parse(xml: XmlData)
	{

	}
}

class MonsterSpreaderAbility : AbstractMonsterAbility()
{
	override fun doCopy(): AbstractMonsterAbility
	{
		val ability = MonsterSpreaderAbility()
		return ability
	}

	override fun activate(entity: Entity, grid: Grid, targets: Array<Tile>)
	{
		val monsterSrc = entity.pos().tile!!

		for (tile in targets)
		{
			val diff = tile.getPosDiff(monsterSrc)
			diff[0].y *= -1

			val spreader = data["SPREADER"] as Spreader
			tile.spreader = spreader.copy()

			tile.spreader!!.spriteWrapper?.chooseSprites()

			val dst = tile.euclideanDist(monsterSrc)
			val animDuration = 0.4f + dst * 0.025f
			val attackSprite = tile.spreader!!.spriteWrapper?.chosenTilingSprite?.copy()
							   ?: tile.spreader!!.spriteWrapper?.chosenSprite?.copy()
							   ?: tile.spreader!!.particleEffect!!.copy()
			attackSprite.animation = LeapAnimation.obtain().set(animDuration, diff, 1f + dst * 0.25f)
			attackSprite.animation = ExpandAnimation.obtain().set(animDuration, 0.5f, 1.5f, false)
			tile.effects.add(attackSprite)

			tile.spreader!!.spriteWrapper?.chosenTilingSprite?.renderDelay = animDuration
			tile.spreader!!.spriteWrapper?.chosenSprite?.renderDelay = animDuration
			tile.spreader!!.particleEffect?.renderDelay = animDuration
		}
	}

	override fun parse(xml: XmlData)
	{

	}

}