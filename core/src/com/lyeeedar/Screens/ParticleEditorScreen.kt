package com.lyeeedar.Screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import com.lyeeedar.Renderables.Animation.MoveAnimation
import com.lyeeedar.Renderables.Particle.ParticleEffect
import com.lyeeedar.Renderables.Particle.ParticleEffectDescription
import com.lyeeedar.Renderables.Renderable
import com.lyeeedar.Renderables.SortedRenderer
import com.lyeeedar.Renderables.Sprite.Sprite
import com.lyeeedar.Renderables.Sprite.TilingSprite
import com.lyeeedar.UI.addClickListener
import com.lyeeedar.Util.*
import ktx.collections.set
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.swing.JColorChooser

/**
 * Created by Philip on 14-Aug-16.
 */

class ParticleEditorScreen : AbstractScreen()
{
	var currentPath: String? = null
	lateinit var particle: ParticleEffect
	val batch = SpriteBatch()
	lateinit var background: Array2D<Symbol>
	lateinit var collision: Array2D<Boolean>
	var tileSize = 32f
	lateinit var spriteRender: SortedRenderer
	val shape = ShapeRenderer()
	var colour: java.awt.Color = java.awt.Color.WHITE
	val crossedTiles = ObjectSet<Point>()
	val particlePos = Point()
	lateinit var debugButton: CheckBox
	lateinit var alignUpButton: CheckBox
	var deltaMultiplier = 1f
	var size = 1

	override fun show()
	{
		if ( !created )
		{
			baseCreate()
			created = true
		}

		Gdx.input.inputProcessor = inputMultiplexer
	}

	override fun create()
	{
		val browseButton = TextButton("...", Statics.skin)
		val updateButton = TextButton("Update", Statics.skin)
		val playbackSpeedBox = SelectBox<Float>(Statics.skin)
		playbackSpeedBox.setItems(0.01f, 0.05f, 0.1f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f, 3f, 4f, 5f)
		playbackSpeedBox.selected = 1f

		playbackSpeedBox.addListener(object : ChangeListener()
		{
			override fun changed(event: ChangeEvent?, actor: Actor?)
			{
				deltaMultiplier = playbackSpeedBox.selected
			}

		})

		val colourButton = TextButton("Colour", Statics.skin)
		colourButton.addClickListener {
			colour = JColorChooser.showDialog(null, "Particle Colour", colour)
			particle.colour.set(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
			colourButton.color = particle.colour.color()
		}

		browseButton.addClickListener {
			val fc = java.awt.FileDialog(Frame(), "Load", FileDialog.LOAD)
			fc.directory = Gdx.files.internal("../assetsraw/Particles").file().absoluteFile.path

			fc.isVisible = true

			val file = fc.file ?: return@addClickListener

			currentPath = File("../assetsraw/Particles/$file").absolutePath

			val rawxml = getRawXml(currentPath!!)
			val xmlData = XmlData.loadFromElement(rawxml)

			val nparticle = ParticleEffect.Companion.load(xmlData, ParticleEffectDescription(currentPath!!))
			nparticle.killOnAnimComplete = false
			nparticle.setPosition(particle.position.x, particle.position.y)
			nparticle.rotation = particle.rotation
			//nparticle.speedMultiplier = playbackSpeedBox.selected
			nparticle.colour.set(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
			particle = nparticle
		}

		updateButton.addClickListener {

			val rawxml = getRawXml(currentPath!!)
			val xmlData = XmlData.loadFromElement(rawxml)

			val nparticle = ParticleEffect.Companion.load(xmlData, ParticleEffectDescription(currentPath!!))

			nparticle.killOnAnimComplete = false
			nparticle.setPosition(particle.position.x, particle.position.y)
			nparticle.rotation = particle.rotation
			//nparticle.speedMultiplier = playbackSpeedBox.selected
			nparticle.colour.set(colour.red / 255f, colour.green / 255f, colour.blue / 255f, colour.alpha / 255f)
			particle = nparticle
		}

		debugButton = CheckBox("Debug", Statics.skin)
		alignUpButton = CheckBox("AlignUp", Statics.skin)

		val sizeBox = SelectBox<Int>(Statics.skin)
		sizeBox.setItems(1, 2, 3, 4, 5)
		sizeBox.selected = 1

		sizeBox.addListener(object : ChangeListener()
									 {
										 override fun changed(event: ChangeEvent?, actor: Actor?)
										 {
											 size = sizeBox.selected
										 }

									 })

		val buttonsTable = Table()
		buttonsTable.add(browseButton).expandY().top()
		buttonsTable.add(updateButton).expandY().top()
		buttonsTable.add(playbackSpeedBox).expandY().top()
		buttonsTable.add(colourButton).expandY().top()
		buttonsTable.row()
		buttonsTable.add(debugButton).expandY().top()
		buttonsTable.add(alignUpButton).expandY().top()
		buttonsTable.add(sizeBox).expandY().top()

		mainTable.add(buttonsTable).growX()
		mainTable.row()

		particle = ParticleEffect(ParticleEffectDescription(""))

		loadLevel()

		val clickTable = Table()
		clickTable.debug()
		clickTable.touchable = Touchable.enabled

		clickTable.addListener(object : InputListener()
							   {
								   override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean
								   {
									   val xp = x + ((spriteRender.width * tileSize) / 2f) - (clickTable.width / 2f)

									   val sx = (xp / tileSize).toInt()
									   val sy = spriteRender.height.toInt() - ((spriteRender.height.toInt()-1) - (y / tileSize).toInt()) - 1

									   val p1 = Vector2(particle.position)
									   val p2 = Vector2(sx.toFloat(), sy.toFloat())

									   particlePos.set(sx, sy)

									   val dist = p1.dst(p2)

									   particle.animation = null
									   particle.animation = MoveAnimation.obtain().set(dist, arrayOf(p1, p2), Interpolation.linear)
									   particle.rotation = getRotation(p1, p2)

									   Point.freeAll(crossedTiles)
									   crossedTiles.clear()
									   particle.collisionFun = fun(x:Int, y:Int) { crossedTiles.add(Point.obtain().set(x, y)) }

									   particle.start()

									   return true
								   }
							   })

		mainTable.add(clickTable).grow()
	}

	fun loadLevel()
	{
		val xml = getXml("Particles/ParticleTestLevel")

		val symbolsEl = xml.getChildByName("Symbols")!!
		val symbolMap = ObjectMap<Char, Symbol>()

		for (i in 0..symbolsEl.childCount-1)
		{
			val el = symbolsEl.getChild(i)
			val symbol = Symbol.load(el)
			symbolMap[symbol.char] = symbol
		}

		val rowsEl = xml.getChildByName("Rows")!!
		val width = rowsEl.getChild(0).text.length
		val height = rowsEl.childCount

		background = Array2D(width, height) { x, y -> symbolMap[rowsEl.getChild(height - y - 1).text[x]].copy() }
		collision = Array2D(width, height) { x, y -> background[x, y].isWall }

		val tilex = Statics.resolution.x.toFloat() / width.toFloat()
		tileSize = tilex

		spriteRender = SortedRenderer(tileSize, width.toFloat(), height.toFloat(), 2, true)
	}

	val tempPoint = Point()
	override fun doRender(delta: Float)
	{
		particle.size[0] = size
		particle.size[1] = size

		if (alignUpButton.isChecked)
		{
			particle.rotation = 0f
		}

		batch.projectionMatrix = stage.camera.combined

		Statics.collisionGrid = collision

		spriteRender.begin(delta * deltaMultiplier, 0f, 0f, Colour.WHITE)

		for (x in 0..background.xSize-1)
		{
			for (y in 0..background.ySize-1)
			{
				val symbol = background[x, y]
				var i = 0
				for (renderable in symbol.sprites)
				{
					tempPoint.set(x, y)
					val col = if (crossedTiles.contains(tempPoint)) Color.GOLD else Color.WHITE

					if (renderable is Sprite)
					{
						spriteRender.queueSprite(renderable, x.toFloat(), y.toFloat(), 0, i++, Colour(col))
					}
					else if (renderable is TilingSprite)
					{
						spriteRender.queueSprite(renderable, x.toFloat(), y.toFloat(), 0, i++, Colour(col))
					}
				}
			}
		}
		spriteRender.queueParticle(particle, particlePos.x.toFloat(), particlePos.y.toFloat(), 1, 0)

		batch.color = Color.WHITE
		spriteRender.end(batch)

		if (debugButton.isChecked)
		{
			shape.projectionMatrix = stage.camera.combined
			shape.setAutoShapeType(true)
			shape.begin()

			particle.debug(shape, 0f, 0f, tileSize, true, true, true)

			shape.end()
		}
	}
}

class Symbol
{
	var char: Char = ' '
	val sprites: Array<Renderable> = Array()
	var isWall: Boolean = false

	fun copy(): Symbol
	{
		val symbol = Symbol()
		symbol.char = char
		for (sprite in sprites)
		{
			symbol.sprites.add(sprite.copy())
		}
		symbol.isWall = isWall

		return symbol
	}

	companion object
	{
		fun load(xml: XmlData) : Symbol
		{
			val symbol = Symbol()
			symbol.isWall = xml.getBooleanAttribute("IsWall", false)

			for (i in 0..xml.childCount-1)
			{
				val el = xml.getChild(i)
				if (el.name == "Char") symbol.char = el.text[0]
				else
				{
					if (el.name == "Sprite")
					{
						symbol.sprites.add(AssetManager.loadSprite(el))
					}
					else if (el.name == "TilingSprite")
					{
						symbol.sprites.add(AssetManager.loadTilingSprite(el))
					}
					else
					{
						throw RuntimeException("Invalid symbol data type '${el.name}'!")
					}
				}
			}

			return symbol
		}
	}
}