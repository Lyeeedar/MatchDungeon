package com.badlogic.gdx.graphics.g2d

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.utils.BufferUtils
import com.badlogic.gdx.utils.GdxRuntimeException
import java.nio.ByteBuffer
import java.nio.IntBuffer

class LargeIndexBufferObject(isStatic: Boolean, maxIndices: Int)
{
	internal val buffer: IntBuffer
	internal val byteBuffer: ByteBuffer
	internal var bufferHandle: Int = 0
	internal val isDirect: Boolean
	internal var isDirty = true
	internal var isBound = false
	internal val usage: Int

	// used to work around bug: https://android-review.googlesource.com/#/c/73175/
	private val empty: Boolean

	/** Creates a new static IndexBufferObject to be used with vertex arrays.
	 *
	 * @param maxIndices the maximum number of indices this buffer can hold
	 */
	constructor(maxIndices: Int) : this(true, maxIndices)
	{
	}

	init
	{
		var maxIndices = maxIndices

		empty = maxIndices == 0
		if (empty)
		{
			maxIndices = 1 // avoid allocating a zero-sized buffer because of bug in Android's ART < Android 5.0
		}

		byteBuffer = BufferUtils.newUnsafeByteBuffer(maxIndices * 4)
		isDirect = true

		buffer = byteBuffer.asIntBuffer()
		buffer.flip()
		byteBuffer.flip()
		bufferHandle = Gdx.gl20.glGenBuffer()
		usage = if (isStatic) GL20.GL_STATIC_DRAW else GL20.GL_DYNAMIC_DRAW
	}

	/** @return the number of indices currently stored in this buffer
	 */
	fun getNumIndices(): Int
	{
		return if (empty) 0 else buffer.limit()
	}

	/** @return the maximum number of indices this IndexBufferObject can store.
	 */
	fun getNumMaxIndices(): Int
	{
		return if (empty) 0 else buffer.capacity()
	}

	/**
	 *
	 *
	 * Sets the indices of this IndexBufferObject, discarding the old indices. The count must equal the number of indices to be
	 * copied to this IndexBufferObject.
	 *
	 *
	 *
	 *
	 * This can be called in between calls to [.bind] and [.unbind]. The index data will be updated instantly.
	 *
	 *
	 * @param indices the vertex data
	 * @param offset the offset to start copying the data from
	 * @param count the number of shorts to copy
	 */
	fun setIndices(indices: IntArray, offset: Int, count: Int)
	{
		isDirty = true
		buffer.clear()
		buffer.put(indices, offset, count)
		buffer.flip()
		byteBuffer.position(0)
		byteBuffer.limit(count shl 1)

		if (isBound)
		{
			Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
			isDirty = false
		}
	}

	fun setIndices(indices: IntBuffer)
	{
		isDirty = true
		val pos = indices.position()
		buffer.clear()
		buffer.put(indices)
		buffer.flip()
		indices.position(pos)
		byteBuffer.position(0)
		byteBuffer.limit(buffer.limit() shl 1)

		if (isBound)
		{
			Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
			isDirty = false
		}
	}

	fun updateIndices(targetOffset: Int, indices: IntArray, offset: Int, count: Int)
	{
		isDirty = true
		val pos = byteBuffer.position()
		byteBuffer.position(targetOffset * 2)
		BufferUtils.copy(indices, offset, byteBuffer, count)
		byteBuffer.position(pos)
		buffer.position(0)

		if (isBound)
		{
			Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
			isDirty = false
		}
	}

	/**
	 *
	 *
	 * Returns the underlying ShortBuffer. If you modify the buffer contents they wil be uploaded on the call to [.bind].
	 * If you need immediate uploading use [.setIndices].
	 *
	 *
	 * @return the underlying short buffer.
	 */
	fun getBuffer(): IntBuffer
	{
		isDirty = true
		return buffer
	}

	/** Binds this IndexBufferObject for rendering with glDrawElements.  */
	fun bind()
	{
		if (bufferHandle == 0) throw GdxRuntimeException("No buffer allocated!")

		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, bufferHandle)
		if (isDirty)
		{
			byteBuffer.limit(buffer.limit() * 2)
			Gdx.gl20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, byteBuffer.limit(), byteBuffer, usage)
			isDirty = false
		}
		isBound = true
	}

	/** Unbinds this IndexBufferObject.  */
	fun unbind()
	{
		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
		isBound = false
	}

	/** Invalidates the IndexBufferObject so a new OpenGL buffer handle is created. Use this in case of a context loss.  */
	fun invalidate()
	{
		bufferHandle = Gdx.gl20.glGenBuffer()
		isDirty = true
	}

	/** Disposes this IndexBufferObject and all its associated OpenGL resources.  */
	fun dispose()
	{
		Gdx.gl20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, 0)
		Gdx.gl20.glDeleteBuffer(bufferHandle)
		bufferHandle = 0

		BufferUtils.disposeUnsafeByteBuffer(byteBuffer)
	}
}
