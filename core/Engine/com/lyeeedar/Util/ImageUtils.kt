package com.lyeeedar.Util

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Pixmap.Format
import com.badlogic.gdx.graphics.Texture

import java.awt.image.BufferedImage

object ImageUtils
{
	fun pixmapToImage(pm: Pixmap): BufferedImage
	{
		val image = BufferedImage(pm.width, pm.height, BufferedImage.TYPE_INT_ARGB)
		for (x in 0 until pm.width)
		{
			for (y in 0 until pm.height)
			{
				val c = Color()
				Color.rgba8888ToColor(c, pm.getPixel(x, y))

				val cc = java.awt.Color(c.r, c.g, c.b, c.a)

				image.setRGB(x, y, cc.rgb)
			}
		}

		return image
	}

	fun textureToPixmap(texture: Texture): Pixmap
	{
		if (!texture.textureData.isPrepared)
		{
			texture.textureData.prepare()
		}

		return texture.textureData.consumePixmap()
	}

	fun multiplyPixmap(image: Pixmap, mask: Pixmap): Pixmap
	{
		val pixmap = Pixmap(image.width, image.height, Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		val cb = Color()
		val ca = Color()

		val xRatio = mask.width.toFloat() / image.width.toFloat()
		val yRatio = mask.height.toFloat() / image.height.toFloat()

		for (x in 0 until image.width)
		{
			for (y in 0 until image.height)
			{
				Color.rgba8888ToColor(ca, image.getPixel(x, y))

				val maskX = (x.toFloat() * xRatio).toInt()
				val maskY = (y.toFloat() * yRatio).toInt()

				Color.rgba8888ToColor(cb, mask.getPixel(maskX, maskY))

				ca.mul(cb)

				pixmap.drawPixel(x, y, Color.rgba8888(ca))
			}
		}

		return pixmap
	}

	fun addPixmap(image: Pixmap, mask: Pixmap): Pixmap
	{
		val pixmap = Pixmap(image.width, image.height, Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		val cb = Color()
		val ca = Color()

		val xRatio = mask.width.toFloat() / image.width.toFloat()
		val yRatio = mask.height.toFloat() / image.height.toFloat()

		for (x in 0 until image.width)
		{
			for (y in 0 until image.height)
			{
				Color.rgba8888ToColor(ca, image.getPixel(x, y))

				val maskX = (x.toFloat() * xRatio).toInt()
				val maskY = (y.toFloat() * yRatio).toInt()

				Color.rgba8888ToColor(cb, mask.getPixel(maskX, maskY))

				ca.add(cb)

				pixmap.drawPixel(x, y, Color.rgba8888(ca))
			}
		}

		return pixmap
	}

	fun flattenImages(images: com.badlogic.gdx.utils.Array<Pair<Pixmap, Boolean>>): Pixmap
	{
		var maxWidth = 0
		var maxHeight = 0

		for (image in images)
		{
			val drawWidth = if (image.second) image.first.width else 32
			val drawHeight = if (image.second) image.first.height else 32

			if (drawWidth > maxWidth)
			{
				maxWidth = drawWidth
			}

			if (drawHeight > maxHeight)
			{
				maxHeight = drawHeight
			}
		}

		val pixmap = Pixmap(maxWidth, maxHeight, Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		val cb = Color()
		val ca = Color()

		for (image in images)
		{
			val drawWidth = if (image.second) image.first.width else 32
			val drawHeight = if (image.second) image.first.height else 32

			val startX = (maxWidth / 2) - (drawWidth / 2)

			val xRatio = image.first.width.toFloat() / drawWidth
			val yRatio = image.first.height.toFloat() / drawHeight

			for (x in 0 until drawWidth)
			{
				for (y in 0 until drawHeight)
				{
					val imgX = (x.toFloat() * xRatio).toInt()
					val imgY = (y.toFloat() * yRatio).toInt()

					Color.rgba8888ToColor(ca, pixmap.getPixel(startX+x, (maxHeight - drawHeight) + y))
					Color.rgba8888ToColor(cb, image.first.getPixel(imgX, imgY))

					val a = ca.a + cb.a
					ca.mul((1f - cb.a))
					cb.mul(cb.a)

					ca.add(cb)
					ca.a = a

					pixmap.drawPixel(startX+x, (maxHeight - drawHeight) + y, Color.rgba8888(ca))
				}
			}
		}

		return pixmap
	}

	fun composeOverhang(base: Pixmap, overhang: Pixmap): Pixmap
	{
		if (base.width != overhang.width || base.height != overhang.height) throw RuntimeException("Incompatible texture sizes for compose overhang!")

		// increase original image by 50% in both axis
		val pixmap = Pixmap((base.width * 1.5f).toInt(), (base.height * 1.5f).toInt(), Format.RGBA8888)

		pixmap.setColor(1f, 1f, 1f, 0f)
		pixmap.fill()

		// draw original to bottom center
		val xOff = base.width / 4
		val yOff = base.height / 2
		for (x in 0 until base.width)
		{
			for (y in 0 until base.height)
			{
				pixmap.drawPixel(xOff + x, yOff + y, base.getPixel(x, y))
			}
		}

		// draw overhang to top center
		for (x in 0 until base.width)
		{
			for (y in 0 until base.height / 2)
			{
				pixmap.drawPixel(xOff + x, y, overhang.getPixel(x, yOff + y))
			}
		}

		return pixmap
	}

	fun resize(input: Pixmap, width: Int, height: Int): Pixmap
	{
		val pixmap = Pixmap(width, height, Format.RGBA8888)

		val xRatio = input.width.toFloat() / width.toFloat()
		val yRatio = input.height.toFloat() / height.toFloat()

		for (x in 0 until width)
		{
			for (y in 0 until height)
			{
				val inputX = (x.toFloat() * xRatio).toInt()
				val inputY = (y.toFloat() * yRatio).toInt()

				pixmap.drawPixel(x, y, input.getPixel(inputX, inputY))
			}
		}

		return pixmap
	}
}
