package com.lyeeedar.UI

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.scenes.scene2d.ui.Widget
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.lyeeedar.Renderables.Sprite.MaskedTextureData

class MaskedTexture(val base: TextureRegion, val mask: TextureRegion, val layer1: TextureRegion, val layer2: TextureRegion, val layer3: TextureRegion) : Widget()
{
	constructor(data: MaskedTextureData) : this(data.base, data.mask, data.layer1, data.layer2, data.layer3)

	val shader = createShader()

	private val scaling = Scaling.fit
	private val align = Align.center
	private var imageX: Float = 0f
	private var imageY: Float = 0f
	private var imageWidth: Float = 0f
	private var imageHeight: Float = 0f

	override fun remove(): Boolean
	{
		shader.dispose()
		return super.remove()
	}

	override fun layout()
	{
		val regionWidth = base.regionWidth.toFloat()
		val regionHeight = base.regionHeight.toFloat()

		val size = scaling.apply(regionWidth, regionHeight, width, height)
		imageWidth = size.x
		imageHeight = size.y

		imageX = (width / 2 - imageWidth / 2).toInt().toFloat()
		imageY = (height / 2 - imageHeight / 2).toInt().toFloat()
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		batch.shader = shader
		batch.color = Color(color.r, color.g, color.b, color.a * parentAlpha)

		batch.draw(base, x + imageX, y + imageY, imageWidth/2f, imageHeight/2f, imageWidth, imageHeight, 1f, 1f, rotation)

		batch.shader = null

		super.draw(batch, parentAlpha)
	}

	fun createShader(): ShaderProgram
	{
		val vertexShader = """

attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;

uniform mat4 u_projTrans;

vec4 originalTexCoords = vec4(${base.u}, ${base.v}, ${base.u2}, ${base.v2});
vec4 maskTexCoords = vec4(${mask.u}, ${mask.v}, ${mask.u2}, ${mask.v2});
vec4 layer1TexCoords = vec4(${layer1.u}, ${layer1.v}, ${layer1.u2}, ${layer1.v2});
vec4 layer2TexCoords = vec4(${layer2.u}, ${layer2.v}, ${layer2.u2}, ${layer2.v2});
vec4 layer3TexCoords = vec4(${layer3.u}, ${layer3.v}, ${layer3.u2}, ${layer3.v2});

varying vec4 v_color;

varying vec2 v_texCoords;
varying vec2 v_maskTexCoords;
varying vec2 v_layer1TexCoords;
varying vec2 v_layer2TexCoords;
varying vec2 v_layer3TexCoords;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};

	v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;

	float uAlpha = (${ShaderProgram.TEXCOORD_ATTRIBUTE}0.x - originalTexCoords.x) / (originalTexCoords.z - originalTexCoords.x);
	float vAlpha = (${ShaderProgram.TEXCOORD_ATTRIBUTE}0.y - originalTexCoords.y) / (originalTexCoords.w - originalTexCoords.y);
	vec2 texAlpha = vec2(uAlpha, vAlpha);

	v_maskTexCoords = mix(maskTexCoords.xy, maskTexCoords.zw, texAlpha);
	v_layer1TexCoords = mix(layer1TexCoords.xy, layer1TexCoords.zw, texAlpha);
	v_layer2TexCoords = mix(layer2TexCoords.xy, layer2TexCoords.zw, texAlpha);
	v_layer3TexCoords = mix(layer3TexCoords.xy, layer3TexCoords.zw, texAlpha);

	gl_Position = u_projTrans * ${ShaderProgram.POSITION_ATTRIBUTE};
}
"""
		val fragmentShader = """

#ifdef GL_ES
#define LOWP lowp
precision mediump float;
#else
#define LOWP
#endif

varying LOWP vec4 v_color;

varying vec2 v_texCoords;
varying vec2 v_maskTexCoords;
varying vec2 v_layer1TexCoords;
varying vec2 v_layer2TexCoords;
varying vec2 v_layer3TexCoords;

uniform sampler2D u_texture;

void main()
{
	vec4 baseSample = texture2D(u_texture, v_texCoords);
	vec4 maskSample = texture2D(u_texture, v_maskTexCoords);
	vec4 layer1Sample = texture2D(u_texture, v_layer1TexCoords);
	vec4 layer2Sample = texture2D(u_texture, v_layer2TexCoords);
	vec4 layer3Sample = texture2D(u_texture, v_layer3TexCoords);

	vec4 masked = (maskSample.r * layer1Sample + maskSample.g * layer2Sample + maskSample.b * layer3Sample) * baseSample;
	vec4 final = mix(baseSample, masked, maskSample.a);

	gl_FragColor = v_color * final;
}

"""

		val shader = ShaderProgram(vertexShader, fragmentShader)
		if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)
		return shader
	}
}