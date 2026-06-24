package com.oayilix.lodestar.gradle

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_FINAL
import org.objectweb.asm.Opcodes.ACC_PRIVATE
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ACC_SUPER
import org.objectweb.asm.Opcodes.ALOAD
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.ASTORE
import org.objectweb.asm.Opcodes.DUP
import org.objectweb.asm.Opcodes.INVOKEINTERFACE
import org.objectweb.asm.Opcodes.INVOKESPECIAL
import org.objectweb.asm.Opcodes.INVOKESTATIC
import org.objectweb.asm.Opcodes.NEW
import org.objectweb.asm.Opcodes.RETURN
import org.objectweb.asm.Opcodes.V1_8

/**
 * Builds the stable aggregate class loaded by the runtime.
 * 生成由 Runtime 加载的稳定聚合类。
 *
 * Registry calls are direct bytecode references, allowing R8 to inline them without reflection.
 * 路由表调用采用直接字节码引用，使 R8 无需反射即可安全内联。
 */
internal object RouterMappingBytecodeBuilder {

    const val CLASS_NAME = "com/oayilix/lodestar/mapping/RouterMapping"

    fun build(registryNames: Collection<String>): ByteArray {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        writer.visit(
            V1_8,
            ACC_PUBLIC or ACC_FINAL or ACC_SUPER,
            CLASS_NAME,
            null,
            "java/lang/Object",
            null
        )

        writer.visitMethod(ACC_PRIVATE, "<init>", "()V", null, null).apply {
            visitCode()
            visitVarInsn(ALOAD, 0)
            visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
            visitInsn(RETURN)
            visitMaxs(0, 0)
            visitEnd()
        }

        writer.visitMethod(
            ACC_PUBLIC or ACC_STATIC,
            "get",
            "()Ljava/util/Map;",
            "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/Class<+Landroid/app/Activity;>;>;",
            null
        ).apply {
            visitCode()
            visitTypeInsn(NEW, "java/util/LinkedHashMap")
            visitInsn(DUP)
            visitMethodInsn(INVOKESPECIAL, "java/util/LinkedHashMap", "<init>", "()V", false)
            visitVarInsn(ASTORE, 0)

            // Sort again at the bytecode boundary to keep output deterministic for every caller.
            // 在字节码边界再次排序，确保任意调用方都得到确定性输出。
            registryNames.sorted().forEach { registryName ->
                visitVarInsn(ALOAD, 0)
                visitMethodInsn(INVOKESTATIC, registryName, "get", "()Ljava/util/Map;", false)
                visitMethodInsn(
                    INVOKEINTERFACE,
                    "java/util/Map",
                    "putAll",
                    "(Ljava/util/Map;)V",
                    true
                )
            }

            visitVarInsn(ALOAD, 0)
            visitMethodInsn(
                INVOKESTATIC,
                "java/util/Collections",
                "unmodifiableMap",
                "(Ljava/util/Map;)Ljava/util/Map;",
                false
            )
            visitInsn(ARETURN)
            visitMaxs(0, 0)
            visitEnd()
        }

        writer.visitEnd()
        return writer.toByteArray()
    }
}
