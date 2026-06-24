package com.oayilix.lodestar.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

abstract class GenerateRouterMappingTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    // 用于去重：记录已经写入输出 jar 的条目名称
    @get:Internal
    val addedEntries = mutableSetOf<String>()

    @TaskAction
    fun execute() {
        val collector = RouterMappingCollector()
        addedEntries.clear()

        // 创建输出 jar 文件
        val outputFile = output.get().asFile
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        JarOutputStream(FileOutputStream(outputFile)).use { jos ->
            // 1、处理所有目录输入（class 文件）
            allDirectories.get().forEach { dir ->
                val dirFile = dir.asFile
                if (dirFile.exists()) {
                    processDirectory(dirFile, jos, collector)
                }
            }

            // 2、处理所有 jar 输入
            allJars.get().forEach { jar ->
                val jarFile = jar.asFile
                if (jarFile.exists()) {
                    processJar(jarFile, jos, collector)
                }
            }

            // 3、生成汇总的 RouterMapping.class 写入输出 jar
            println("$name all mapping class name = ${collector.getMappingClassNames()}")

            val entryName = RouterMappingBytecodeBuilder.CLASS_NAME + ".class"
            val entry = ZipEntry(entryName)
            jos.putNextEntry(entry)
            jos.write(RouterMappingBytecodeBuilder.get(collector.getMappingClassNames()))
            jos.closeEntry()

            println("$name wrote RouterMapping.class to output jar, total entries = ${addedEntries.size}")
        }
    }

    private fun processDirectory(dir: File, jos: JarOutputStream, collector: RouterMappingCollector) {
        dir.walkTopDown().filter { it.isFile }.forEach { file ->
            // 收集映射表类名（始终收集，不跳过去重）
            collector.collect(file)

            // 去重
            val relativePath = dir.toPath().relativize(file.toPath()).toString()
            if (!addedEntries.add(relativePath)) return@forEach

            // 拷贝到输出 jar
            val entry = ZipEntry(relativePath)
            jos.putNextEntry(entry)
            jos.write(file.readBytes())
            jos.closeEntry()
        }
    }

    private fun processJar(jarFile: File, jos: JarOutputStream, collector: RouterMappingCollector) {
        val jar = JarFile(jarFile)
        jar.use {
            // 从 jar 中收集映射表类名
            collector.collectFromJarFile(jarFile)

            // 拷贝所有条目到输出 jar
            val entries = it.entries()
            while (entries.hasMoreElements()) {
                val jarEntry = entries.nextElement()
                if (jarEntry.isDirectory) continue

                // 去重：跳过 MANIFEST.MF 和已添加的条目
                if (jarEntry.name == "META-INF/MANIFEST.MF" || !addedEntries.add(jarEntry.name)) {
                    continue
                }

                val outEntry = ZipEntry(jarEntry.name)
                jos.putNextEntry(outEntry)
                it.getInputStream(jarEntry).use { input ->
                    input.copyTo(jos)
                }
                jos.closeEntry()
            }
        }
    }
}
