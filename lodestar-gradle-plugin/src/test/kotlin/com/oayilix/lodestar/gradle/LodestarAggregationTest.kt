package com.oayilix.lodestar.gradle

import org.gradle.api.GradleException
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes.ACC_PUBLIC
import org.objectweb.asm.Opcodes.ACC_STATIC
import org.objectweb.asm.Opcodes.ACONST_NULL
import org.objectweb.asm.Opcodes.ARETURN
import org.objectweb.asm.Opcodes.POP
import org.objectweb.asm.Opcodes.V1_8

class LodestarAggregationTest {

    @Test
    fun aggregateBytecodeIsDeterministic() {
        val first = LodestarMappingBytecodeBuilder.build(listOf("mapping/B", "mapping/A"))
        val second = LodestarMappingBytecodeBuilder.build(listOf("mapping/A", "mapping/B"))
        assertArrayEquals(first, second)
    }

    @Test
    fun duplicateRoutesAcrossRegistriesFailTheBuild() {
        val collector = LodestarMappingCollector()
        collector.collect(
            "com/oayilix/lodestar/mapping/LodestarRegistry_a.class",
            registryBytecode("com/oayilix/lodestar/mapping/LodestarRegistry_a", "lodestar://example.com/a")
        )

        assertThrows(GradleException::class.java) {
            collector.collect(
                "com/oayilix/lodestar/mapping/LodestarRegistry_b.class",
                registryBytecode("com/oayilix/lodestar/mapping/LodestarRegistry_b", "lodestar://example.com/a")
            )
        }
    }

    private fun registryBytecode(internalName: String, route: String): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(V1_8, ACC_PUBLIC, internalName, null, "java/lang/Object", null)
        writer.visitMethod(ACC_PUBLIC or ACC_STATIC, "get", "()Ljava/util/Map;", null, null).apply {
            visitCode()
            visitLdcInsn(route)
            visitInsn(POP)
            visitInsn(ACONST_NULL)
            visitInsn(ARETURN)
            visitMaxs(1, 0)
            visitEnd()
        }
        writer.visitEnd()
        return writer.toByteArray()
    }
}
