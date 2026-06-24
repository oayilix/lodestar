package com.oayilix.lodestar.gradle

import java.util.jar.JarEntry
import java.util.jar.JarFile

class RouterMappingCollector {

    private static final String PACKAGE_NAME = "com/oayilix/lodestar/mapping"
    private static final String CLASS_NAME_PREFIX = "RouterMapping_"
    private static final String CLASS_FILE_SUFFIX = ".class"

    private final Set<String> mappingClassNames = new HashSet<>()

    /**
     * 获取收集到的映射表类名
     * @return
     */
    Set<String> getMappingClassNames() {
        return mappingClassNames
    }

    /**
     * 收集传递进来的 class 文件或者 class 文件目录中的映射表类
     * @param classFile
     */
    void collect(File classFile) {
        if (classFile == null || !classFile.exists()) return
        if (classFile.isFile()) {
            // 是 class 文件
            if (classFile.absolutePath.contains(PACKAGE_NAME)
                    && classFile.name.startsWith(CLASS_NAME_PREFIX)
                    && classFile.name.endsWith(CLASS_FILE_SUFFIX)) {
                // 同时满足：1、绝对路径包含包名。2、文件名为"RouterMapping_"开头。3、文件名以".class"结尾。
                String className = classFile.name.replace(CLASS_FILE_SUFFIX, "")
                mappingClassNames.add(className)
            }
        } else {
            // 是一个目录
            classFile.listFiles().each {
                collect(it)
            }
        }
    }

    /**
     * 收集 jar 包中的映射表类
     * @param jarFile
     */
    void collectFromJarFile(File jarFile) {
        Enumeration enumeration = new JarFile(jarFile).entries()

        while (enumeration.hasMoreElements()) {
            JarEntry jarEntry = enumeration.nextElement()
            String entryName = jarEntry.name

            if (entryName.contains(PACKAGE_NAME)
                    && entryName.contains(CLASS_NAME_PREFIX)
                    && entryName.contains(CLASS_FILE_SUFFIX)) {
                String className = entryName
                        .replace(PACKAGE_NAME, "")
                        .replace("/", "")
                        .replace(CLASS_FILE_SUFFIX, "")
                mappingClassNames.add(className)
            }
        }
    }
}