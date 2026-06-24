package com.oayilix.lodestar.gradle

import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.*

object RouterMappingBytecodeBuilder : Opcodes {

    const val CLASS_NAME = "com/oayilix/lodestar/mapping/RouterMapping"

    fun get(allMappingNames: Set<String>): ByteArray {
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        var methodVisitor: MethodVisitor

        // 创建类
        classWriter.visit(
            V1_8,
            ACC_PUBLIC or ACC_SUPER,
            CLASS_NAME,
            null,
            "java/lang/Object",
            null
        )

        classWriter.visitSource("RouterMapping.java", null)

        // 创建构造方法
        methodVisitor = classWriter.visitMethod(
            ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        methodVisitor.visitCode()
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitMethodInsn(
            INVOKESPECIAL,
            "java/lang/Object",
            "<init>",
            "()V",
            false
        )
        methodVisitor.visitInsn(RETURN)
        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()

        // 创建 get() 方法
        methodVisitor = classWriter.visitMethod(
            ACC_PUBLIC or ACC_STATIC,
            "get",
            "()Ljava/util/Map;",
            "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
            null
        )
        methodVisitor.visitCode()

        methodVisitor.visitTypeInsn(NEW, "java/util/HashMap")
        methodVisitor.visitInsn(DUP)
        methodVisitor.visitMethodInsn(
            INVOKESPECIAL,
            "java/util/HashMap",
            "<init>",
            "()V",
            false
        )
        methodVisitor.visitVarInsn(ASTORE, 0)

        // 向汇总映射表中装入所有子工程生成的映射表
        allMappingNames.forEach { name ->
            methodVisitor.visitVarInsn(ALOAD, 0)
            methodVisitor.visitMethodInsn(
                INVOKESTATIC,
                "com/oayilix/lodestar/mapping/$name",
                "get",
                "()Ljava/util/Map;",
                false
            )
            methodVisitor.visitMethodInsn(
                INVOKEINTERFACE,
                "java/util/Map",
                "putAll",
                "(Ljava/util/Map;)V",
                true
            )
        }
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitInsn(ARETURN)
        methodVisitor.visitMaxs(2, 1)

        methodVisitor.visitEnd()
        classWriter.visitEnd()

        return classWriter.toByteArray()
    }
}
