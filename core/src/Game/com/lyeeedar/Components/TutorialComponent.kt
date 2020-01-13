package com.lyeeedar.Components

import com.badlogic.ashley.core.ComponentMapper
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Pool
import com.lyeeedar.Board.Grid
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.XmlData

fun Entity.tutorial(): TutorialComponent? = TutorialComponent.mapper.get(this)
class TutorialComponent : AbstractComponent()
{
	var displayTutorial: ((grid: Grid, entity: Entity, gridWidget: GridWidget)->Tutorial?)? = null

	var obtained: Boolean = false
	companion object
	{
		val mapper: ComponentMapper<TutorialComponent> = ComponentMapper.getFor(TutorialComponent::class.java)
		fun get(entity: Entity): TutorialComponent? = mapper.get(entity)

		private val pool: Pool<TutorialComponent> = object : Pool<TutorialComponent>() {
			override fun newObject(): TutorialComponent
			{
				return TutorialComponent()
			}

		}

		@JvmStatic fun obtain(): TutorialComponent
		{
			val obj = pool.obtain()

			if (obj.obtained) throw RuntimeException()
			obj.reset()

			obj.obtained = true
			return obj
		}
	}
	override fun free() { if (obtained) { pool.free(this); obtained = false } }

	override fun parse(xml: XmlData, entity: Entity, parentPath: String)
	{
		TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
	}

	override fun reset()
	{
		displayTutorial = null
	}
}