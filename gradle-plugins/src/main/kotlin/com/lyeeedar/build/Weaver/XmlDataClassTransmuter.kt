package com.lyeeedar.build

import org.objectweb.asm.*
import org.objectweb.asm.util.Textifier
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter

class XmlDataClassTransmuter(val file: String, val cr: ClassReader, val meta: ClassMetadata) : Opcodes
{
	fun process()
	{
		val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)

		try
		{
			val cv = CommonClassWeaver(cw, meta)
			val lw = LoadWeaver(cv, meta)
			cr.accept(lw, ClassReader.EXPAND_FRAMES)
			writeClass(cw, file)
		}
		catch (e: Exception)
		{
			e.printStackTrace()
		}
	}

	fun writeClass(writer: ClassWriter, file: String)
	{
		var fos: FileOutputStream? = null
		try
		{
			fos = FileOutputStream(file)
			fos.write(writer.toByteArray())
		}
		catch (e: IOException)
		{
			e.printStackTrace()
		}
		finally
		{
			if (fos != null) try
			{
				fos.close()
			}
			catch (e: IOException)
			{
				e.printStackTrace()
			}
		}
	}
}

class CommonClassWeaver(cv: ClassVisitor, val meta: ClassMetadata) : ClassVisitor(Opcodes.ASM4, cv), Opcodes
{
	override fun visit(version: Int, access: Int, name: String?, signature: String?, superName: String?, interfaces: Array<out String>?)
	{
		cv.visit(version, access, name, signature, "com/lyeeedar/XmlDataClass", interfaces)
	}
}

class LoadWeaver(cv: ClassVisitor, val meta: ClassMetadata) : ClassVisitor(Opcodes.ASM4, cv), Opcodes
{
	override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<String?>?): MethodVisitor
	{
		var mv: MethodVisitor = cv.visitMethod(access, name, descriptor, signature, exceptions)
		if (name == "load" && descriptor == "()V")
		{
			val textifier = Textifier()
			textifier.visitMethod(access, name, descriptor, signature, exceptions)

			val writer = StringWriter()
			PrintWriter(writer).use({ pw -> textifier.print(pw) })
			println(writer.toString())

			mv = LoadMethodVisitor(mv, meta)
		}
		return mv
	}
}

class LoadMethodVisitor(mv: MethodVisitor, val meta: ClassMetadata) : MethodVisitor(Opcodes.ASM4, mv), Opcodes
{
	override fun visitCode()
	{
		mv.visitCode()
		for (field in meta.instanceFields())
		{
			loadField(field)
		}
	}

	private fun loadField(field: FieldDescriptor)
	{
		mv.visitVarInsn(Opcodes.ALOAD, 0)
		field.defaultValue!!.accept(mv)
		mv.visitFieldInsn(Opcodes.PUTFIELD, meta.type!!.internalName, field.name, field.desc)
	}
}