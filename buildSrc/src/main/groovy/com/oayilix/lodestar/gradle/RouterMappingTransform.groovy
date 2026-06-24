package com.oayilix.lodestar.gradle

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformException
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils

import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class RouterMappingTransform extends Transform {

    /**
     * 返回当前 Transform 名称，这个名称会被打印到 gradle 的日志里面
     * @return
     */
    @Override
    String getName() {
        return "RouterMappingTransform"
    }

    /**
     * 返回对象的作用是用来告知编译器，当前 Transform 需要消费的输入类型。
     * 也就是我们需要编译器帮我们传入的对象的类型。
     * 这里我们要处理的对象是 class，所以要求编译器安徽 class 类型。
     * @return
     */
    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS
    }

    /**
     * 用来告诉编译器，当前的 Transform 需要作用的范围是在哪里。
     * 是整个工程还是当前子工程。
     * @return
     */
    @Override
    Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    /**
     * 告诉编译器单签 Transform 是否支持增量
     * 通常直接返回 false
     * @return
     */
    @Override
    boolean isIncremental() {
        return false
    }

    /**
     * 当编译器把所有的 class 都收集好以后，会将它们打包成为 TransformInvocation
     * 然后通过这个方法将打包好的结果回调给我们
     * 所以我们就可以在这个方法里面对回调给我们的 class 作二次处理。
     * @param transformInvocation
     * @throws TransformException
     * @throws InterruptedException
     * @throws IOException
     */
    @Override
    void transform(TransformInvocation transformInvocation)
            throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)
        // 1、遍历所有的 input
        // 2、对 input 进行二次处理
        // 3、将 input 拷贝到目标目录

        RouterMappingCollector collector = new RouterMappingCollector()

        transformInvocation.inputs.each {
            // 把工程中文件夹类型的输入拷贝到目标目录
            it.directoryInputs.each {directoryInput ->
                def destDir = transformInvocation.outputProvider
                        .getContentLocation(
                                directoryInput.name,
                                directoryInput.contentTypes,
                                directoryInput.scopes,
                                Format.DIRECTORY)
                collector.collect(directoryInput.file)
                FileUtils.copyDirectory(directoryInput.file, destDir)
            }
            // 把工程中 jar 类型的输入拷贝到目标目录
            it.jarInputs.each {jarInput ->
                def dest = transformInvocation.outputProvider
                        .getContentLocation(
                                jarInput.name,
                                jarInput.contentTypes,
                                jarInput.scopes,
                                Format.JAR)
                collector.collectFromJarFile(jarInput.file)
                FileUtils.copyFile(jarInput.file, dest)
            }
        }

        println(getName() + " all mapping class name = " + collector.mappingClassNames)

        // 将生成的字节码写入文件
        File mappingJarFile = transformInvocation.outputProvider
                .getContentLocation(
                        "router_mapping",
                        getOutputTypes(),
                        getScopes(),
                        Format.JAR
                )   // 得到即将生成的 jar 包存放的位置
        println(getName() + " mappingJarFile = " + mappingJarFile)
        if (!mappingJarFile.getParentFile().exists()) {
            mappingJarFile.getParentFile().mkdirs()
        }
        if (mappingJarFile.exists()) {
            mappingJarFile.delete()
        }
        FileOutputStream fileOutPutStream = new FileOutputStream(mappingJarFile)
        JarOutputStream jarOutputStream = new JarOutputStream(fileOutPutStream)
        ZipEntry zipEntry = new ZipEntry(RouterMappingBytecodeBuilder.CLASS_NAME + ".class")
        jarOutputStream.putNextEntry(zipEntry)
        jarOutputStream.write(RouterMappingBytecodeBuilder.get(collector.mappingClassNames))
        jarOutputStream.closeEntry()
        jarOutputStream.close()
        fileOutPutStream.close()
    }
}