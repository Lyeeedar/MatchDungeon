package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Board.OrbDesc
import com.lyeeedar.Util.XmlData

fun Entity.matchable(): MatchableComponent? = MatchableComponent.mapper.get(this)
class MatchableComponent : AbstractComponent()
{
	var desc: OrbDesc = OrbDesc()
		private set(value)
		{
			field = value
		}

	fun setDesc(desc: OrbDesc, entity: Entity)
	{
		this.desc = desc
		entity.renderable().renderable.colour = desc.sprite.colour
	}

	var canMatch: Boolean = true
	var markedForDeletion: Boolean = false
	var deletionEffectDelay: Float = 0f

	var isChanger: Boolean = false
		private set(value)
		{
			field = value
		}
	var nextDesc: OrbDesc? = null

	fun setIsChanger(nextDesc: OrbDesc)
	{
		isChanger = true
		this.nextDesc = nextDesc
	}

	var skipPowerOrb = false

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<MatchableComponent> = ComponentMapper.getFor(MatchableComponent::class.java)
		fun get(entity: Entity): MatchableComponent? = mapper.get(entity)

		private val pool: Pool<MatchableComponent> = object : Pool<MatchableComponent>() {
			override fun newObject(): MatchableComponent
			{
				return MatchableComponent()
			}

		}

		@JvmStatic fun obtain(): MatchableComponent
		{
			val obj = MatchableComponent.pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { MatchableComponent.pool.free(this); obtained = false } }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset()
	{
		isChanger = false
		nextDesc = null
		canMatch = true
		markedForDeletion = false
		deletionEffectDelay = 0f
		skipPowerOrb = false
	}
}