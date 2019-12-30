package com.lyeeedar.UI

import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.ObjectMap
import com.lyeeedar.Util.Random
import com.lyeeedar.Util.max
import ktx.collections.set

class DissolveEffect(val drawable: Table, val duration: Float, val gradient: TextureRegion, val smoothness: Float) : Actor()
{
	var time = 0f
	var dissolvePoint: Vector2? = null
	var maxDissolveDist: Float = 0f

	override fun act(delta: Float)
	{
		time += delta
		if (time >= duration)
		{
			remove()
		}
	}

	override fun draw(batch: Batch, parentAlpha: Float)
	{
		if (dissolvePoint == null)
		{
			dissolvePoint = Vector2(x + Random.random() * width, y + Random.random() * height)

			maxDissolveDist = max(maxDissolveDist, Vector2.dst(dissolvePoint!!.x, dissolvePoint!!.y, x, y))
			maxDissolveDist = max(maxDissolveDist, Vector2.dst(dissolvePoint!!.x, dissolvePoint!!.y, x + width, y))
			maxDissolveDist = max(maxDissolveDist, Vector2.dst(dissolvePoint!!.x, dissolvePoint!!.y, x, y + height))
			maxDissolveDist = max(maxDissolveDist, Vector2.dst(dissolvePoint!!.x, dissolvePoint!!.y, x + width, y + height))
		}

		batch.shader = shader

		shader.setUniformf("u_timeAlpha", 1f - (time / duration))
		shader.setUniformf("u_dissolvePoint", dissolvePoint!!)
		shader.setUniformf("u_maxDissolveRange", maxDissolveDist)
		shader.setUniformf("u_tableScale", drawable.scaleX)
		batch.setColor(color.r, color.g, color.b, color.a * parentAlpha)
		drawable.draw(batch, color.a * parentAlpha)

		batch.shader = null
	}

	val shader = createShader(smoothness, gradient)

	companion object
	{
		val shaders = ObjectMap<String, ShaderProgram>()
		fun createShader(smoothness: Float, gradient: TextureRegion): ShaderProgram
		{
			val vertexShader = """

attribute vec4 ${ShaderProgram.POSITION_ATTRIBUTE};
attribute vec4 ${ShaderProgram.COLOR_ATTRIBUTE};
attribute vec2 ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;

uniform mat4 u_projTrans;

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_pos;

void main()
{
	v_color = ${ShaderProgram.COLOR_ATTRIBUTE};
	v_texCoords = ${ShaderProgram.TEXCOORD_ATTRIBUTE}0;

	v_pos = ${ShaderProgram.POSITION_ATTRIBUTE}.xy;

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

uniform float u_timeAlpha;
uniform vec2 u_dissolvePoint;
uniform float u_maxDissolveRange;
uniform float u_tableScale;

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_pos;

uniform sampler2D u_texture;

vec4 gradientTexCoords = vec4(${gradient.u}, ${gradient.v}, ${gradient.u2}, ${gradient.v2});

vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

float snoise(vec2 v)
{
	const vec4 C = vec4(0.211324865405187, 0.366025403784439, -0.577350269189626, 0.024390243902439);
	vec2 i  = floor(v + dot(v, C.yy) );
	vec2 x0 = v -   i + dot(i, C.xx);
	  vec2 i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
	  vec4 x12 = x0.xyxy + C.xxzz;
	  x12.xy -= i1;
	  i = mod(i, 289.0);
	  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
	  + i.x + vec3(0.0, i1.x, 1.0 ));
	  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);
	  m = m*m ;
	  m = m*m ;
	  vec3 x = 2.0 * fract(p * C.www) - 1.0;
	  vec3 h = abs(x) - 0.5;
	  vec3 ox = floor(x + 0.5);
	  vec3 a0 = x - ox;
	  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
	  vec3 g;
	  g.x  = a0.x  * x0.x  + h.x  * x0.y;
	  g.yz = a0.yz * x12.xz + h.yz * x12.yw;
	  return 130.0 * dot(m, g);
}

float noise(vec2 pos)
{
	float factor1 = 0.1 / $smoothness;
	float factor3 = 0.3 / $smoothness;
	float factor10 = 0.05 * $smoothness;
	float factor6 = 1.0 - (factor10 + factor3 + factor1);

	return
		snoise(pos / (70.0 / u_tableScale)) * factor10 +
		snoise(pos / (30.0 / u_tableScale)) * factor6 +
		snoise(pos / (10.0 / u_tableScale)) * factor3 +
		snoise(pos / (5.0 / u_tableScale)) * factor1;
}

float noiseCardinal(vec2 pos, float offset)
{
	float total = 0.0;

	total += noise(pos + vec2(offset, 0));
	total += noise(pos + vec2(0, offset));
	total += noise(pos + vec2(-offset, 0));
	total += noise(pos + vec2(0, -offset));

	return total / 4.0;
}

float noiseDiamond(vec2 pos, float offset)
{
	float total = 0.0;

	total += noise(pos + vec2(offset, offset));
	total += noise(pos + vec2(offset, -offset));
	total += noise(pos + vec2(-offset, offset));
	total += noise(pos + vec2(-offset, -offset));

	return total / 4.0;
}

float smoothNoise()
{
	float total = noise(v_pos);

	//total = total * 0.6 + noiseCardinal(v_pos, 2.0) * 0.4;
	//total = total * 0.7 + noiseDiamond(v_pos, 2.0) * 0.3;

	//total = total * 0.8 + noiseCardinal(v_pos, 4.0) * 0.2;
	//total = total * 0.9 + noiseDiamond(v_pos, 4.0) * 0.1;

	return (total + 1.0) * 0.5;
}

void main()
{
	vec4 diffuseSample = texture2D(u_texture, v_texCoords);

	float noiseVal = smoothNoise();

	float toPoint = ((length(v_pos - u_dissolvePoint) * u_tableScale) / ((1.0001 - u_timeAlpha) * u_maxDissolveRange));
	float diff = ((u_timeAlpha + noiseVal) * toPoint) - 1.0;

	float overOne = clamp(diff * 1.0, 0.0, 1.0);

	if (diff < 0.0)
	{
		diffuseSample.a = 0.0;
	}
	else
	{
		vec4 gradientSample = texture2D(u_texture, mix(gradientTexCoords.xy, gradientTexCoords.zw, overOne));

		float alpha = gradientSample.a;
		gradientSample.a = diffuseSample.a;

		diffuseSample = mix(diffuseSample, gradientSample, alpha);
	}

	gl_FragColor = v_color * diffuseSample;
}

"""

			val key = vertexShader + fragmentShader
			if (shaders.containsKey(key))
			{
				return shaders[key]
			}

			val shader = ShaderProgram(vertexShader, fragmentShader)
			if (!shader.isCompiled) throw IllegalArgumentException("Error compiling shader: " + shader.log)

			shaders[key] = shader

			return shader
		}
	}
}