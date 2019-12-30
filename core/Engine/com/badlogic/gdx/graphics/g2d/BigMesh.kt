package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.graphics.glutils.VertexBufferObject

class BigMesh
{
	internal var vertices: VertexBufferObject
	internal var indices: LargeIndexBufferObject

	constructor(isStatic: Boolean, maxVertices: Int, maxIndices: Int, vararg attributes: VertexAttribute)
	{
		vertices = VertexBufferObject(isStatic, maxVertices, VertexAttributes(*attributes))
		indices = LargeIndexBufferObject(true, maxIndices)
	}

	fun setIndices(indices: IntArray): BigMesh
	{
		this.indices.setIndices(indices, 0, indices.size)

		return this
	}

	fun setVertices(vertices: FloatArray, offset: Int, count: Int): BigMesh
	{
		this.vertices.setVertices(vertices, offset, count)

		return this
	}

	fun updateVertices(vertices: FloatArray, offset: Int, count: Int)
	{
		this.vertices.updateVertices(offset, vertices, offset, count)
	}

	fun bind(shader: ShaderProgram)
	{
		vertices.bind(shader, null)
		indices.bind()
	}

	fun unbind(shader: ShaderProgram)
	{
		vertices.unbind(shader, null)
		indices.unbind()
	}

	fun render(shader: ShaderProgram, primitiveType: Int, offset: Int, count: Int)
	{
		if (count == 0) return

		//bind(shader)

		Gdx.gl20.glDrawElements(primitiveType, count, GL20.GL_UNSIGNED_INT, offset * 4)

		//unbind(shader)
	}

	fun dispose()
	{
		vertices.dispose()
		indices.dispose()
	}
}