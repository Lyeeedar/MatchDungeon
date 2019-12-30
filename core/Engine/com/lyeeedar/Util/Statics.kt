package com.lyeeedar.Util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Colors
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.ObjectMap
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.MainGame
import com.lyeeedar.Screens.AbstractScreen
import com.lyeeedar.UI.*
import com.lyeeedar.UI.Tooltip
import ktx.collections.set

class Statics
{
	companion object
	{
		val PARTICLE_EDITOR = false

		lateinit var skin: Skin
		var fps = 60
		var android = false
		val release = false
		lateinit var applicationChanger: AbstractApplicationChanger
		var resolution = Point(360, 640)
		var screenSize = Point(resolution.x, resolution.y)

		var collisionGrid: Array2D<Boolean>? = null

		lateinit var controls: Controls

		lateinit var game: MainGame

		val stage: Stage
			get() = (game.screen as AbstractScreen).stage

		val debugConsole: DebugConsole
			get() = (game.screen as AbstractScreen).debugConsole

		var settings = Settings()

		private fun loadSkin(): Skin
		{
			val skin = Skin()

			val smallfont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 8, Color(0.97f, 0.87f, 0.7f, 1f), 1, Color.BLACK, false)
			skin.add("small", smallfont)

			val font = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 12, Color(0.97f, 0.87f, 0.7f, 1f), 1, Color.BLACK, false)
			skin.add("default", font)

			val cardfont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 12, Color(0.0f, 0.0f, 0.0f, 1f), 0, Color.BLACK, false)
			skin.add("card", cardfont)

			val cardwhitefont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 12, Color(1.0f, 1.0f, 1.0f, 1f), 0, Color.BLACK, false)
			skin.add("cardwhite", cardwhitefont)

			val textButtonCardfont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 12, Color(0.97f, 0.87f, 0.7f, 1f), 0, Color.BLACK, false)
			skin.add("textButtonCard", textButtonCardfont)

			val titlefont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 20, Color(1f, 0.9f, 0.8f, 1f), 1, Color.BLACK, true)
			skin.add("title", titlefont)

			val cardTitlefont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 18, Color(0f, 0.0f, 0.0f, 1f), 0, Color.BLACK, false)
			skin.add("cardtitle", cardTitlefont)

			val popupfont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 20, Color(1f, 1f, 1f, 1f), 1, Color.DARK_GRAY, true)
			skin.add("popup", popupfont)

			val consolefont = AssetManager.loadFont("Sprites/Unpacked/font.ttf", 8, Color(0.9f, 0.9f, 0.9f, 1f), 0, Color.BLACK, false)
			skin.add("console", consolefont)

			val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
			pixmap.setColor(Color.WHITE)
			pixmap.fill()
			skin.add("white", Texture(pixmap))

			val buttonBackground = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/Button.png"), 6, 6, 6, 6))
			val buttonCardBackground = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/ButtonCard.png"), 6, 6, 6, 6))

			val textField = TextField.TextFieldStyle()
			textField.fontColor = Color.WHITE
			textField.font = skin.getFont("default")
			textField.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6))
			textField.focusedBackground = (textField.background as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			textField.cursor = skin.newDrawable("white", Color.WHITE)
			textField.selection = skin.newDrawable("white", Color.LIGHT_GRAY)
			skin.add("default", textField)

			val consoleText = TextField.TextFieldStyle()
			consoleText.fontColor = Color.WHITE
			consoleText.font = skin.getFont("console")
			consoleText.background = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/white.png")).tint(Color(0.1f, 0.1f, 0.1f, 0.6f))
			consoleText.focusedBackground = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/white.png")).tint(Color(0.3f, 0.3f, 0.3f, 0.6f))
			consoleText.cursor = skin.newDrawable("white", Color.WHITE)
			consoleText.selection = skin.newDrawable("white", Color.LIGHT_GRAY)
			skin.add("console", consoleText)

			val consolelabel = Label.LabelStyle()
			consolelabel.font = skin.getFont("console")
			skin.add("console", consolelabel)

			val label = Label.LabelStyle()
			label.font = skin.getFont("default")
			skin.add("default", label)

			val titleLabel = Label.LabelStyle()
			titleLabel.font = skin.getFont("title")
			skin.add("title", titleLabel)

			val popupLabel = Label.LabelStyle()
			popupLabel.font = skin.getFont("popup")
			skin.add("popup", popupLabel)

			val smallLabel = Label.LabelStyle()
			smallLabel.font = skin.getFont("small")
			skin.add("small", smallLabel)

			val cardLabel = Label.LabelStyle()
			cardLabel.font = skin.getFont("card")
			skin.add("card", cardLabel)

			val cardWhiteLabel = Label.LabelStyle()
			cardWhiteLabel.font = skin.getFont("cardwhite")
			skin.add("cardwhite", cardWhiteLabel)

			val cardTitleLabel = Label.LabelStyle()
			cardTitleLabel.font = skin.getFont("cardtitle")
			skin.add("cardtitle", cardTitleLabel)

			val checkButton = CheckBox.CheckBoxStyle()
			checkButton.checkboxOff = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/GUI/Unchecked.png"))
			checkButton.checkboxOn = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/GUI/Checked.png"))
			checkButton.font = skin.getFont("default")
			checkButton.fontColor = Color.LIGHT_GRAY
			checkButton.overFontColor = Color.WHITE
			skin.add("default", checkButton)

			val textButton = TextButton.TextButtonStyle()
			textButton.up = buttonBackground
			textButton.font = skin.getFont("default")
			textButton.fontColor = Color.LIGHT_GRAY
			textButton.overFontColor = Color.WHITE
			//textButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
			textButton.over = (textButton.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("default", textButton)

			val textCardButton = TextButton.TextButtonStyle()
			textCardButton.up = buttonCardBackground
			textCardButton.font = skin.getFont("textButtonCard")
			textCardButton.fontColor = Color.LIGHT_GRAY
			textCardButton.overFontColor = Color.WHITE
			//textCardButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
			textCardButton.over = (textCardButton.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("defaultcard", textCardButton)

			val bigTextButton = TextButton.TextButtonStyle()
			bigTextButton.up = buttonBackground
			bigTextButton.font = skin.getFont("title")
			bigTextButton.fontColor = Color.LIGHT_GRAY
			bigTextButton.overFontColor = Color.WHITE
			//bigTextButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
			bigTextButton.over = (bigTextButton.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("big", bigTextButton)

			val keyBindingButton = TextButton.TextButtonStyle()
			keyBindingButton.up = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6))
			keyBindingButton.font = skin.getFont("default")
			keyBindingButton.fontColor = Color.LIGHT_GRAY
			keyBindingButton.overFontColor = Color.WHITE
			//textButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
			keyBindingButton.over = (keyBindingButton.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("keybinding", keyBindingButton)

			val responseButton = TextButton.TextButtonStyle()
			responseButton.up = buttonBackground
			responseButton.font = skin.getFont("default")
			responseButton.fontColor = Color.LIGHT_GRAY
			responseButton.overFontColor = Color.WHITE
			//textButton.checked = new NinePatchDrawable( new NinePatch( AssetManager.loadTextureRegion( "Sprites/GUI/ButtonDown.png" ), 12, 12, 12, 12 ) );
			responseButton.over = (responseButton.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("responseButton", responseButton)

			val toolTip = Tooltip.TooltipStyle()
			toolTip.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/Tooltip.png"), 21, 21, 21, 21))
			skin.add("default", toolTip)

			val progressBar = ProgressBar.ProgressBarStyle()
			progressBar.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6))
			progressBar.knobBefore = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/ProgressIndicator.png"), 8, 8, 8, 8))
			skin.add("default-horizontal", progressBar)

			val buttonStyle = Button.ButtonStyle()
			buttonStyle.up = buttonBackground
			buttonStyle.over = (buttonStyle.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("default", buttonStyle)

			val buttonCardStyle = Button.ButtonStyle()
			buttonCardStyle.up = buttonCardBackground
			buttonCardStyle.over = (buttonStyle.up as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("defaultcard", buttonCardStyle)

			val closeButton = Button.ButtonStyle()
			closeButton.up = LayeredDrawable(
				buttonBackground,
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_681.png")).tint(Color(0.97f, 0.87f, 0.7f, 1f)))
			closeButton.over = LayeredDrawable(
				buttonBackground.tint(Color.LIGHT_GRAY),
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_681.png")).tint(Color(0.87f, 0.77f, 0.6f, 1f)))
			skin.add("close", closeButton)

			val closeCardButton = Button.ButtonStyle()
			closeCardButton.up = LayeredDrawable(
				buttonCardBackground,
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_681.png")).tint(Color(0.97f, 0.87f, 0.7f, 1f)))
			closeCardButton.over = LayeredDrawable(
				buttonCardBackground.tint(Color.LIGHT_GRAY),
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_681.png")).tint(Color(0.87f, 0.77f, 0.6f, 1f)))
			skin.add("closecard", closeCardButton)

			val infoButton = Button.ButtonStyle()
			infoButton.up = LayeredDrawable(
				buttonBackground,
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_573.png")).tint(Color(0.97f, 0.87f, 0.7f, 1f)))
			infoButton.over = LayeredDrawable(
				buttonBackground.tint(Color.LIGHT_GRAY),
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_573.png")).tint(Color(0.87f, 0.77f, 0.6f, 1f)))
			skin.add("info", infoButton)

			val leftButton = Button.ButtonStyle()
			leftButton.up = LayeredDrawable(
				buttonBackground,
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_787.png")).tint(Color(0.97f, 0.87f, 0.7f, 1f)))
			leftButton.over = LayeredDrawable(
				buttonBackground.tint(Color.LIGHT_GRAY),
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_787.png")).tint(Color(0.87f, 0.77f, 0.6f, 1f)))
			skin.add("left", leftButton)

			val rightButton = Button.ButtonStyle()
			rightButton.up = LayeredDrawable(
				buttonBackground,
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_785.png")).tint(Color(0.97f, 0.87f, 0.7f, 1f)))
			rightButton.over = LayeredDrawable(
				buttonBackground.tint(Color.LIGHT_GRAY),
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_785.png")).tint(Color(0.87f, 0.77f, 0.6f, 1f)))
			skin.add("right", rightButton)


			val infoCardButton = Button.ButtonStyle()
			infoCardButton.up = LayeredDrawable(
				buttonCardBackground,
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_573.png")).tint(Color(0.97f, 0.87f, 0.7f, 1f)))
			infoCardButton.over = LayeredDrawable(
				buttonCardBackground.tint(Color.LIGHT_GRAY),
				TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/Oryx/uf_split/uf_interface/uf_interface_573.png")).tint(Color(0.87f, 0.77f, 0.6f, 1f)))
			skin.add("infocard", infoCardButton)

			val horiCardSeperatorStyle = Seperator.SeperatorStyle()
			horiCardSeperatorStyle.vertical = false
			horiCardSeperatorStyle.thickness = 6
			horiCardSeperatorStyle.background = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/GUI/SeperatorHorizontalCard.png"))
			skin.add("horizontalcard", horiCardSeperatorStyle)

			val horiSeperatorStyle = Seperator.SeperatorStyle()
			horiSeperatorStyle.vertical = false
			horiSeperatorStyle.thickness = 6
			horiSeperatorStyle.background = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/GUI/SeperatorHorizontal.png"))
			skin.add("horizontal", horiSeperatorStyle)

			val vertSeperatorStyle = Seperator.SeperatorStyle()
			vertSeperatorStyle.vertical = true
			vertSeperatorStyle.thickness = 6
			vertSeperatorStyle.background = TextureRegionDrawable(AssetManager.loadTextureRegion("Sprites/GUI/SeperatorVertical.png"))
			skin.add("vertical", vertSeperatorStyle)

			val scrollPaneStyle = ScrollPane.ScrollPaneStyle()
			scrollPaneStyle.vScroll = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6))
			scrollPaneStyle.vScrollKnob = buttonBackground
			skin.add("default", scrollPaneStyle)

			val listStyle = com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle()
			listStyle.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/Tooltip.png"), 21, 21, 21, 21))
			listStyle.font = skin.getFont("default")
			listStyle.selection = skin.newDrawable("white", Color.LIGHT_GRAY)
			skin.add("default", listStyle)

			val selectBoxStyle = SelectBox.SelectBoxStyle()
			selectBoxStyle.fontColor = Color.WHITE
			selectBoxStyle.font = skin.getFont("default")
			selectBoxStyle.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6))
			selectBoxStyle.scrollStyle = scrollPaneStyle
			selectBoxStyle.listStyle = listStyle
			selectBoxStyle.backgroundOver = (selectBoxStyle.background as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			skin.add("default", selectBoxStyle)

			val sliderStyle = Slider.SliderStyle()
			sliderStyle.background = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6))
			sliderStyle.knob = buttonBackground
			sliderStyle.knobOver = (sliderStyle.knob as NinePatchDrawable).tint(Color(0.9f, 0.9f, 0.9f, 1.0f))
			sliderStyle.knobDown = (sliderStyle.knob as NinePatchDrawable).tint(Color.LIGHT_GRAY)
			skin.add("default-horizontal", sliderStyle)

			val tabPanelStyle = TabPanel.TabPanelStyle()
			tabPanelStyle.font = skin.getFont("default")
			tabPanelStyle.fontColor = Color.LIGHT_GRAY
			tabPanelStyle.overFontColor = Color.WHITE
			tabPanelStyle.bodyBackground = NinePatchDrawable(NinePatch(AssetManager.loadTextureRegion("Sprites/GUI/TextField.png"), 6, 6, 6, 6)).tint(Color(1f, 1f, 1f, 0.2f))
			tabPanelStyle.titleButtonUnselected = buttonBackground
			tabPanelStyle.titleButtonSelected = (tabPanelStyle.titleButtonUnselected as NinePatchDrawable).tint(Color(0.8f, 0.8f, 0.8f, 1.0f))
			skin.add("default", tabPanelStyle)

			val horiBack = AssetManager.loadTextureRegion("Sprites/GUI/PanelHorizontal.png")
			val vertBack = AssetManager.loadTextureRegion("Sprites/GUI/PanelVertical.png")

			return skin
		}

		fun setup()
		{
			skin = loadSkin()
			controls = Controls()

			Colors.put("IMPORTANT", Color(0.6f, 1f, 0.9f, 1f))
		}
	}
}

class Settings
{
	val data = ObjectMap<String, Any>()

	fun hasKey(key: String) = data.containsKey(key)
	fun <T> get(key: String, default: T) = if (data.containsKey(key)) data[key] as T else default
	fun set(key: String, value: Any) { data[key] = value }

	fun set(other: Settings)
	{
		for (pair in other.data)
		{
			data[pair.key] = pair.value
		}
	}

	fun save(kryo: Kryo, output: Output)
	{
		kryo.writeObject(output, data)
	}

	companion object
	{
		fun load(kryo: Kryo, input: Input): Settings
		{
			val settings = Settings()

			val newData = kryo.readObject(input, ObjectMap::class.java)

			for (pair in newData)
			{
				settings.data[pair.key as String] = pair.value
			}

			return settings
		}
	}
}