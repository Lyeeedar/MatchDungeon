package com.kryo

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import com.lyeeedar.Util.registerGdxSerialisers
import com.lyeeedar.Util.registerLyeeedarSerialisers

val kryo: Kryo by lazy { initKryo() }
fun initKryo(): Kryo
{
	val kryo = Kryo()
	kryo.isRegistrationRequired = false

	kryo.registerGdxSerialisers()
	kryo.registerLyeeedarSerialisers()

	return kryo
}

fun serialize(obj: Any, path: String)
{
	val outputFile = Gdx.files.local(path)

	var output: Output? = null
	try
	{
		output = Output(outputFile.write(false))
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		return
	}

	kryo.writeClassAndObject(output, obj)

	output.close()
}

fun deserialize(path: String): Any?
{
	var input: Input? = null

	var data: Any?
	try
	{
		input = Input(Gdx.files.local(path).read())
		data = kryo.readClassAndObject(input)
	}
	catch (e: Exception)
	{
		e.printStackTrace()
		data = null
	}
	finally
	{
		input?.close()
	}

	return data
}