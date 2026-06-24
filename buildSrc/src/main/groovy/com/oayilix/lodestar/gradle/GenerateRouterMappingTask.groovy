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

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

abstract class GenerateRouterMappingTask extends DefaultTask {

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ListProperty<RegularFile> getAllJars()

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ListProperty<Directory> getAllDirectories()

    @OutputFile
    abstract RegularFileProperty getOutput()

    // 用于去重：记录已经写入输出 jar 的条目名称
    @Internal
    Set<String> addedEntries = new HashSet<>()

    @TaskAction
    void execute() {
        RouterMappingCollector collector = new RouterMappingCollector()
        addedEntries.clear()

        // 创建输出 jar 文件
        File outputFile = getOutput().get().asFile
        if (!outputFile.parentFile.exists()) {
            outputFile.parentFile.mkdirs()
        }

        JarOutputStream jos = new JarOutputStream(new FileOutputStream(outputFile))
        try {
            // 1、处理所有目录输入（class 文件）
            getAllDirectories().get().each { dir ->
                File dirFile = dir.asFile
                if (dirFile.exists()) {
                    processDirectory(dirFile, jos, collector)
                }
            }

            // 2、处理所有 jar 输入
            getAllJars().get().each { jar ->
                File jarFile = jar.asFile
                if (jarFile.exists()) {
                    processJar(jarFile, jos, collector)
                }
            }

            // 3、生成汇总的 RouterMapping.class 写入输出 jar（即使已存在也覆盖）
            println(getName() + " all mapping class name = " + collector.mappingClassNames)

            String entryName = RouterMappingBytecodeBuilder.CLASS_NAME + ".class"
            ZipEntry entry = new ZipEntry(entryName)
            jos.putNextEntry(entry)
            jos.write(RouterMappingBytecodeBuilder.get(collector.mappingClassNames))
            jos.closeEntry()

            println(getName() + " wrote RouterMapping.class to output jar, total entries = " + addedEntries.size())
        } finally {
            jos.close()
        }
    }

    /**
     * 处理 class 文件目录，将每个文件拷贝到输出 jar 中，
     * 同时收集 RouterMapping_* 类名
     */
    void processDirectory(File dir, JarOutputStream jos, RouterMappingCollector collector) {
        dir.eachFileRecurse { file ->
            if (file.isFile()) {
                // 收集映射表类名（始终收集，不跳过去重）
                collector.collect(file)

                // 去重：如果已添加过同名条目则跳过
                String relativePath = dir.toPath().relativize(file.toPath()).toString()
                if (!addedEntries.add(relativePath)) {
                    return
                }

                // 拷贝到输出 jar
                ZipEntry entry = new ZipEntry(relativePath)
                jos.putNextEntry(entry)
                jos.write(file.bytes)
                jos.closeEntry()
            }
        }
    }

    /**
     * 处理 jar 包，将其中的每个条目拷贝到输出 jar 中，
     * 同时收集 RouterMapping_* 类名
     */
    void processJar(File jarFile, JarOutputStream jos, RouterMappingCollector collector) {
        JarFile jar = new JarFile(jarFile)
        try {
            // 从 jar 中收集映射表类名（始终收集，不跳过去重）
            collector.collectFromJarFile(jarFile)

            // 拷贝所有条目到输出 jar
            Enumeration enumeration = jar.entries()
            while (enumeration.hasMoreElements()) {
                ZipEntry jarEntry = enumeration.nextElement()
                if (!jarEntry.isDirectory()) {
                    // 去重：跳过 META-INF/MANIFEST.MF 和已添加的条目
                    if (jarEntry.name.equals("META-INF/MANIFEST.MF") || !addedEntries.add(jarEntry.name)) {
                        continue
                    }

                    ZipEntry outEntry = new ZipEntry(jarEntry.name)
                    jos.putNextEntry(outEntry)
                    InputStream inputStream = jar.getInputStream(jarEntry)
                    byte[] buffer = new byte[4096]
                    int bytesRead
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        jos.write(buffer, 0, bytesRead)
                    }
                    inputStream.close()
                    jos.closeEntry()
                }
            }
        } finally {
            jar.close()
        }
    }
}
