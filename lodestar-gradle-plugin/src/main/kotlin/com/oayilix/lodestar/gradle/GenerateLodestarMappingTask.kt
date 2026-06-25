package com.oayilix.lodestar.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.util.TreeMap
import java.util.TreeSet
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Collects module registries, validates the merged classpath, and emits the aggregate route class.
 * 收集模块路由表、校验合并后的 classpath，并输出聚合路由类。
 *
 * Inputs and ZIP entries are sorted and timestamps are fixed to make the output reproducible.
 * 输入与 ZIP 条目均稳定排序，并固定时间戳，以确保输出可复现。
 */
@CacheableTask
abstract class GenerateLodestarMappingTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun generate() {
        val collector = LodestarMappingCollector()
        val entries = TreeMap<String, ByteArray>()
        val services = TreeMap<String, TreeSet<String>>()

        // Process directories and JARs in stable order before writing the transformed artifact.
        // 写入转换产物前，按稳定顺序处理目录与 JAR。
        allDirectories.get().map { it.asFile }.sortedBy(File::getAbsolutePath).forEach { directory ->
            if (!directory.exists()) return@forEach
            directory.walkTopDown().filter(File::isFile).forEach { file ->
                val name = directory.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/')
                addEntry(name, file.readBytes(), entries, services, collector)
            }
        }

        allJars.get().map { it.asFile }.sortedBy(File::getAbsolutePath).forEach { jarFile ->
            if (!jarFile.exists()) return@forEach
            JarFile(jarFile).use { jar ->
                jar.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .sortedBy { it.name }
                    .forEach { jarEntry ->
                        val bytes = jar.getInputStream(jarEntry).use { it.readBytes() }
                        addEntry(jarEntry.name, bytes, entries, services, collector)
                    }
            }
        }

        // Service descriptors require semantic merging; keeping only the first file would silently
        // remove providers from downstream libraries.
        // Service 描述文件需要语义合并；仅保留首个文件会静默丢失依赖库中的 provider。
        services.forEach { (name, implementations) ->
            entries[name] = implementations.joinToString(separator = "\n", postfix = "\n")
                .toByteArray(Charsets.UTF_8)
        }
        entries["${LodestarMappingBytecodeBuilder.CLASS_NAME}.class"] =
            LodestarMappingBytecodeBuilder.build(collector.registryNames())

        val outputFile = output.get().asFile
        outputFile.parentFile.mkdirs()
        JarOutputStream(FileOutputStream(outputFile)).use { jar ->
            entries.forEach { (name, bytes) ->
                jar.putNextEntry(ZipEntry(name).apply { time = 0L })
                jar.write(bytes)
                jar.closeEntry()
            }
        }

        logger.info("Generated Lodestar aggregate with ${collector.registryNames().size} registries.")
    }

    private fun addEntry(
        rawName: String,
        bytes: ByteArray,
        entries: MutableMap<String, ByteArray>,
        services: MutableMap<String, TreeSet<String>>,
        collector: LodestarMappingCollector
    ) {
        val name = rawName.replace('\\', '/')
        if (name == "${LodestarMappingBytecodeBuilder.CLASS_NAME}.class" || isSignatureFile(name)) return

        collector.collect(name, bytes)

        if (name.startsWith(SERVICES_PREFIX)) {
            services.getOrPut(name) { TreeSet() }.addAll(
                bytes.toString(Charsets.UTF_8)
                    .lineSequence()
                    .map(String::trim)
                    .filter { it.isNotEmpty() && !it.startsWith('#') }
                    .toList()
            )
            return
        }

        // Identical duplicates are harmless, but conflicting artifacts must fail instead of being
        // resolved by input order.
        // 内容相同的重复条目无害；内容冲突时必须失败，不能依赖输入顺序决定结果。
        val previous = entries.putIfAbsent(name, bytes)
        if (previous != null && !previous.contentEquals(bytes)) {
            throw GradleException("Conflicting class artifact '$name' while aggregating Lodestar routes.")
        }
    }

    private fun isSignatureFile(name: String): Boolean {
        // Repacking invalidates upstream JAR signatures, so signature metadata must not be copied.
        // 重新打包会使上游 JAR 签名失效，因此不能复制签名元数据。
        if (name.equals("META-INF/MANIFEST.MF", ignoreCase = true)) return true
        if (!name.startsWith("META-INF/", ignoreCase = true)) return false
        val extension = name.substringAfterLast('.', missingDelimiterValue = "").uppercase()
        return extension in SIGNATURE_EXTENSIONS
    }

    private companion object {
        const val SERVICES_PREFIX = "META-INF/services/"
        val SIGNATURE_EXTENSIONS = setOf("SF", "RSA", "DSA", "EC")
    }
}
