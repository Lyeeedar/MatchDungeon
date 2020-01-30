package com.lyeeedar.Components

import com.lyeeedar.Board.Grid
import com.lyeeedar.UI.GridWidget
import com.lyeeedar.UI.Tutorial
import com.lyeeedar.Util.XmlData

inline fun Entity.tutorial(): TutorialComponent? = this.components[ComponentType.Tutorial] as TutorialComponent?
class TutorialComponent : AbstractComponent()
{
	override val type: ComponentType = ComponentType.Tutorial

	var displayTutorial: ((grid: Grid, entity: Entity, gridWidget: GridWidget)->Tutorial?)? = null

	override fun parse(xml: XmlData, entity: Entity, parentPath: String) {}

	override fun reset()
	{
		displayTutorial = null
	}
}