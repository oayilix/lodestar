package com.oayilix.lodestar.gradle

import java.io.File
import java.util.jar.JarFile

class RouterMappingCollector {

    private val mappingClassNames = mutableSetOf<String>()

    fun getMappingClassNames(): Set<String> = mappingClassNames

    /**
     * 收集传递进来的 class 文件或者 class 文件目录中的映射表类
     */
    fun collect(classFile: File?) {
        if (classFile == null || !classFile.exists()) return
        if (classFile.isFile) {
            if (classFile.absolutePath.contains(PACKAGE_NAME)
                && classFile.name.startsWith(CLASS_NAME_PREFIX)
                && classFile.name.endsWith(CLASS_FILE_SUFFIX)
            ) {
                val className = classFile.name.replace(CLASS_FILE_SUFFIX, "")
                mappingClassNames.add(className)
            }
        } else {
            classFile.listFiles()?.forEach { collect(it) }
        }
    }

    /**
     * 收集 jar 包中的映射表类
     */
    fun collectFromJarFile(jarFile: File) {
        val jar = JarFile(jarFile)
        jar.use {
            val enumeration = it.entries()
            while (enumeration.hasMoreElements()) {
                val jarEntry = enumeration.nextElement()
                val entryName = jarEntry.name

                if (entryName.contains(PACKAGE_NAME)
                    && entryName.contains(CLASS_NAME_PREFIX)
                    && entryName.contains(CLASS_FILE_SUFFIX)
                ) {
                    val className = entryName
                        .replace(PACKAGE_NAME, "")
                        .replace("/", "")
                        .replace(CLASS_FILE_SUFFIX, "")
                    mappingClassNames.add(className)
                }
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com/oayilix/lodestar/mapping"
        private const val CLASS_NAME_PREFIX = "RouterMapping_"
        private const val CLASS_FILE_SUFFIX = ".class"
    }
}
