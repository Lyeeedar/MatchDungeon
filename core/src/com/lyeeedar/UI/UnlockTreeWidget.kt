package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.Pools
import com.lyeeedar.Game.Save
import com.lyeeedar.Global
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Util.AssetManager
import com.lyeeedar.Util.Colour
import com.lyeeedar.Util.XmlData
import com.lyeeedar.Util.getXml
import ktx.collections.set

/**
 * Created by Philip on 05-Aug-16.
 */

val radiusStep = 60f

class UnlockTreeWidget<T : Unlockable>(val unlockTree: UnlockTree<T>) : Widget()
{
	val lineVec1 = Vector2()
	val lineVec2 = Vector2()
	val tempVec = Vector2()
	val shape = ShapeRenderer()
	val iconSize = 40f
	val circleCol = Color(1f, 1f, 1f, 0.2f)
	val background = AssetManager.loadTexture("Sprites/Oryx/uf_split/uf_terrain/floor_crusted_grey_1.png", wrapping = Texture.TextureWrap.Repeat)
	val unbought = AssetManager.loadSprite("Icons/Unknown", colour = Colour.LIGHT_GRAY)
	val boughtLineCol = Color(Color.GOLDENROD)
	val unboughtLineCol = Color(0.5f, 0.5f, 0.5f, 1f)

	val min = Vector2()
	val max = Vector2()
	var maxDepth = 0

	init
	{
		touchable = Touchable.enabled

		fun getMinMax(item: UnlockableTreeItem<T>, depth: Int)
		{
			if (item.location.x < min.x) min.x = item.location.x
			if (item.location.x > max.x) max.x = item.location.x
			if (item.location.y < min.y) min.y = item.location.y
			if (item.location.y > max.y) max.y = item.location.y
			if (depth > maxDepth) maxDepth = depth

			for (child in item.children) getMinMax(child, depth+1)
		}

		for (item in unlockTree.unlockableItems)
		{
			if (item != null) getMinMax(item, 1)
		}

		addListener(object : ClickListener() {
			override fun clicked(event: InputEvent?, x: Float, y: Float)
			{
				super.clicked(event, x, y)

				for (item in unlockTree.visibleDescendants())
				{
					val dst = item.location.dst(x - (width * 0.5f + 100), y - (height * 0.5f + 100))

					if (dst < iconSize)
					{
						if (item.bought)
						{
							var message = item.data.description

							if (item.data.upgrades != null)
							{
								message += "\n\n[GOLD]Upgrades ${item.data.upgrades}\n"
							}

							MessageBox(item.data.name, message, Pair("Okay", {}))
						}
						else
						{
							val title = "Unknown"
							var message = item.data.unboughtDescription
							message += "\n\nCost:\n"

							var canBuy = true

							val gold = Math.min(Global.player.gold, item.data.buyCost)
							message += "Gold: $gold/${item.data.buyCost}\n"

							if (gold < item.data.buyCost) canBuy = false

							if (item.data.upgrades != null)
							{
								message += "\n[GOLD]Upgrades ${item.data.upgrades}\n"
							}

							if (canBuy)
							{
								MessageBox(title, message, Pair("Buy",
										{
											item.bought = true
											Global.player.gold -= item.data.buyCost
											Save.save()

										}), Pair("Okay", {}))
							}
							else
							{
								MessageBox(title, message, Pair("Okay", {}))
							}
						}

						break
					}
				}
			}
		})
	}

	override fun getPrefWidth(): Float = (max.x - min.x) + 200
	override fun getPrefHeight(): Float = (max.y - min.y) + 200

	override fun draw(batch: Batch?, parentAlpha: Float)
	{
		super.draw(batch, parentAlpha)

		tempVec.set(x + width * 0.5f + 100, y + height * 0.5f + 100)

		batch?.color = Color.LIGHT_GRAY
		batch?.draw(background, 0f, 0f, width, height, -x / 64f, -y / 64f, (-x + width) / 64f, (-y + height) / 64f)
		batch?.color = Color.WHITE

		// draw lines
		fun drawLines(item: UnlockableTreeItem<T>)
		{
			if (!item.bought) return

			for (child in item.children)
			{
				lineVec1.set(item.location).add(tempVec)
				lineVec2.set(child.location).add(tempVec)

				shape.color = if(child.bought) boughtLineCol else unboughtLineCol

				shape.line(lineVec1, lineVec2)

				drawLines(child)
			}
		}

		fun drawIcons(item: UnlockableTreeItem<T>)
		{
			val x = tempVec.x + item.location.x - iconSize * 0.5f
			val y = tempVec.y + item.location.y - iconSize * 0.5f

			if (item.bought)
			{
				item.data.icon.render(batch as SpriteBatch, x, y, iconSize, iconSize)

				for (child in item.children) drawIcons(child)
			}
			else
			{
				unbought.render(batch as SpriteBatch, x, y, iconSize, iconSize)
			}
		}

		batch?.end()
		batch?.begin()

		shape.projectionMatrix = stage.camera.combined
		shape.setAutoShapeType(true)
		shape.begin()
		for (item in unlockTree.unlockableItems)
		{
			if (item != null)
			{
				lineVec1.set(tempVec)
				lineVec2.set(item.location).add(tempVec)

				shape.color = if(item.bought) boughtLineCol else unboughtLineCol

				shape.line(lineVec1, lineVec2)

				drawLines(item)
			}
		}
		shape.end()

		batch?.end()
		batch?.begin()

		unlockTree.baseIcon.render(batch as SpriteBatch, tempVec.x - iconSize * 0.5f, tempVec.y - iconSize * 0.5f, iconSize, iconSize)

		// draw icons
		for (item in unlockTree.unlockableItems)
		{
			if (item != null)
			{
				drawIcons(item)
			}
		}
	}
}

class UnlockTree<T: Unlockable>
{
	lateinit var baseIcon: Sprite

	val unlockableItems = com.badlogic.gdx.utils.Array<UnlockableTreeItem<T>>()

	fun assignLocations()
	{
		val radiansRemaining = Math.PI.toFloat() * 2f
		val radiansStart = Math.toRadians(45.0).toFloat()

		// split rest between skills, max 90 degrees a chunk
		val radiansPerItem = Math.min(Math.PI.toFloat() / 2f, radiansRemaining / unlockableItems.size)

		for (i in 0..unlockableItems.size-1)
		{
			val start = radiansStart + i * radiansPerItem
			unlockableItems[i].assignLocation(radiansPerItem, start, radiusStep)
		}
	}

	fun descendants() : IntMap<UnlockableTreeItem<T>>
	{
		val map = IntMap<UnlockableTreeItem<T>>()
		for (item in unlockableItems) item.descendants(map)

		return map
	}

	fun boughtDescendants() : ObjectMap<String, T>
	{
		val map = ObjectMap<String, T>()
		for (item in unlockableItems) item.boughtDescendants(map)

		return map
	}

	fun visibleDescendants() : com.badlogic.gdx.utils.Array<UnlockableTreeItem<T>>
	{
		val items = com.badlogic.gdx.utils.Array<UnlockableTreeItem<T>>()

		for (item in unlockableItems)
		{
			item.visibleDescendants(items)
		}

		return items
	}

	companion object
	{
		fun <T: Unlockable> load(path: String, factory: () -> T): UnlockTree<T>
		{
			val xml = getXml("$path")

			val resources = ObjectMap<String, XmlData>()
			val unlockables = ObjectMap<String, T>()
			val treeMap = ObjectMap<String, UnlockableTreeItem<T>>()
			val childMap = ObjectMap<String, Array<String>>()

			val resourcesEl = xml.getChildByName("Resources")
			if (resourcesEl != null)
			{
				for (i in 0 until resourcesEl.childCount)
				{
					val el = resourcesEl.getChild(i)
					resources[el.getAttribute("Key")] = el
				}
			}

			val unlockablesEl = xml.getChildByName("Abilities")!!
			for (i in 0 until unlockablesEl.childCount)
			{
				val el = unlockablesEl.getChild(i)
				val key = el.get("Key")

				val unlockable = factory()
				unlockable.load(el, resources)

				unlockables[key] = unlockable
				treeMap[key] = UnlockableTreeItem(unlockable, key.hashCode())

				val childEl = el.getChildByName("Children")

				val children = Array<String>()
				if (childEl != null)
				{
					for (ii in 0 until childEl.childCount)
					{
						children.add(childEl.getChild(ii).text)
					}
				}
				childMap[key] = children
			}

			for (key in unlockables.keys())
			{
				val tree = treeMap[key]
				val children = childMap[key]

				for (child in children)
				{
					tree.children.add(treeMap[child])
				}
			}

			val treeEl = xml.getChildByName("Tree")!!

			val tree = UnlockTree<T>()
			tree.baseIcon = AssetManager.tryLoadSpriteWithResources(xml.getChildByName("Icon")!!, resources)

			for (i in 0 until treeEl.childCount)
			{
				val el = treeEl.getChild(i)
				val unlockableItem = treeMap[el.text]

				tree.unlockableItems.add(unlockableItem)
			}

			tree.assignLocations()
			return tree
		}
	}
}

class UnlockableTreeItem<T: Unlockable>(val data: T, val guid: Int)
{
	var bought = false

	val location: Vector2 = Vector2()
	val children: com.badlogic.gdx.utils.Array<UnlockableTreeItem<T>> = com.badlogic.gdx.utils.Array()

	fun visibleDescendants(array: com.badlogic.gdx.utils.Array<UnlockableTreeItem<T>>)
	{
		array.add(this)

		if (!bought) return

		for (child in children)
		{
			child?.visibleDescendants(array)
		}
	}

	fun descendants(map: IntMap<UnlockableTreeItem<T>>)
	{
		map[guid] = this
		for (child in children) child?.descendants(map)
	}

	fun boughtDescendants(map: ObjectMap<String, T>)
	{
		if (!bought) return

		map[data.key] = data

		for (child in children) child?.boughtDescendants(map)
	}

	fun assignLocation(angle: Float, start: Float, radius: Float)
	{
		val drawAngle = start + angle * 0.5f

		val tempVec = Pools.obtain(Vector2::class.java)
		tempVec.set(0f, 1f)
		tempVec.rotateRad(drawAngle)
		tempVec.scl(radius)

		location.set(tempVec)
		Pools.free(tempVec)

		if (children.size > 0)
		{
			val angleStep = angle / children.size
			val childRadius = radius + radiusStep * Math.max(1, children.size-1)
			for (i in 0..children.size-1)
			{
				children[i].assignLocation(angleStep, start + i*angleStep, childRadius )
			}
		}
	}
}

abstract class Unlockable()
{
	lateinit var name: String
	lateinit var description: String
	lateinit var unboughtDescription: String
	var buyCost = 1
	var upgrades: String? = null

	var key: String = ""
		get() = upgrades ?: field

	lateinit var icon: Sprite

	fun load(xml: XmlData, resources: ObjectMap<String, XmlData>)
	{
		key = xml.get("Key")

		val dataEl = xml.getChildByName("AbilityData")!!

		name = dataEl.get("Name")
		description = dataEl.get("Description")

		buyCost = dataEl.getInt("BuyCost", 0)

		unboughtDescription = dataEl.get("UnboughtDescription", description)!!
		upgrades = dataEl.get("Upgrades", null)

		icon = AssetManager.tryLoadSpriteWithResources(dataEl.getChildByName("Icon")!!, resources)

		parse(dataEl, resources)
	}

	protected abstract fun parse(xml: XmlData, resources: ObjectMap<String, XmlData>)
	open fun stats(): String? = null
}