package com.lyeeedar.build

import org.objectweb.asm.*
import java.io.FileOutputStream
import java.io.IOException

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

	fun writeClass(writer: ClassWriter, file: String?)
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
		cv.visit(version, access, name, signature, "com/lyeeedar/utils/XmlDataClass", interfaces)
	}
}

class LoadWeaver(cv: ClassVisitor, val meta: ClassMetadata) : ClassVisitor(Opcodes.ASM4, cv), Opcodes
{
	override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<String?>?): MethodVisitor
	{
		var method: MethodVisitor = cv.visitMethod(access, name, desc, signature, exceptions)
		//if ("<init>" == name) method = ConstructorInvocationVisitor(method, meta)
		//if ("reset" == name && "()V" == desc) method = ResetMethodVisitor(method, meta)
		return method
	}

}