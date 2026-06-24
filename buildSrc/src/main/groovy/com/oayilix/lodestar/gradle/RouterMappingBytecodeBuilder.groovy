package com.oayilix.lodestar.gradle


import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class RouterMappingBytecodeBuilder implements Opcodes {

    public static final String CLASS_NAME = "com/oayilix/lodestar/mapping/RouterMapping"

    static byte[] get(Set<String> allMappingNames) {
        // 1、创建一个类
        // 2、创建一个构造方法（手动生成字节码的时候，构造方法需要由我们手动创建）
        // 3、创建一个 get() 方法
        //  1）创建一个 map
        //  2）向 map 中装入所有映射表的内容
        //  3）返回 map

        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS)
        MethodVisitor methodVisitor

        // 创建类
        classWriter.visit(V1_8,
                ACC_PUBLIC | ACC_SUPER,
                CLASS_NAME,
                null,
                "java/lang/Object",
                null)

        classWriter.visitSource("RouterMapping.java", null);

        // 创建构造方法
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC,
                "<init>",
                "()V",
                null,
                null)
        methodVisitor.visitCode()   // 开启字节码的生成或访问，下面开始写字节码指令

        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitMethodInsn(INVOKESPECIAL,
                "java/lang/Object",
                "<init>",
                "()V",
                false)
        methodVisitor.visitInsn(RETURN)

        methodVisitor.visitMaxs(1, 1)
        methodVisitor.visitEnd()    // 关闭字节码的生成或访问

        // 创建 get() 方法
        methodVisitor = classWriter.visitMethod(
                ACC_PUBLIC | ACC_STATIC,
                "get",
                "()Ljava/util/Map;",
                "()Ljava/util/Map<Ljava/lang/String;Ljava/lang/String;>;",
                null)
        methodVisitor.visitCode()

        methodVisitor.visitTypeInsn(NEW, "java/util/HashMap")   // 创建一个 map
        methodVisitor.visitInsn(DUP)    // 将其入栈
        methodVisitor.visitMethodInsn(INVOKESPECIAL,
                "java/util/HashMap",
                "<init>",
                "()V",
                false)  // 入栈后调用 HashMap 的构造方法得到 HashMap 的实例
        methodVisitor.visitVarInsn(ASTORE, 0)   // 将 map 保存起来

        // 向汇总映射表中装入所有子工程生成的映射表
        allMappingNames.each {
            methodVisitor.visitVarInsn(ALOAD, 0)
            methodVisitor.visitMethodInsn(INVOKESTATIC,
                    "com/oayilix/lodestar/mapping/$it",
                    "get",
                    "()Ljava/util/Map;",
                    false)
            methodVisitor.visitMethodInsn(INVOKEINTERFACE,
                    "java/util/Map",
                    "putAll",
                    "(Ljava/util/Map;)V",
                    true)
        }
        methodVisitor.visitVarInsn(ALOAD, 0)
        methodVisitor.visitInsn(ARETURN)
        methodVisitor.visitMaxs(2, 1)

        methodVisitor.visitEnd()
        classWriter.visitEnd()

        return classWriter.toByteArray();
    }

}