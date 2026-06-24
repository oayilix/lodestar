package com.oayilix.lodestar.gradle

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        // 当采用 apply 关键字在工程里面去引用插件的时候，apply 方法里面的逻辑将会被执行
        // 所以这里可以写注入插件的逻辑，比如往工程里面动态添加 task
        println("RouterPlugin, apply from $project.name")

        // 判断当前工程是否有 com.android.application
        if (project.plugins.hasPlugin(AppPlugin)) {
            // 使用新的 androidComponents API 替代已废弃的 Transform API
            ApplicationAndroidComponentsExtension androidComponents =
                    project.extensions.getByType(ApplicationAndroidComponentsExtension)

            androidComponents.onVariants(androidComponents.selector().all()) { variant ->
                // 注册一个 Task 来生成汇总的 RouterMapping.class
                def taskProvider = project.tasks.register(
                        "generateRouterMapping${variant.name.capitalize()}",
                        GenerateRouterMappingTask
                )

                // 使用 ScopedArtifacts API 将 Task 接入 AGP 构建流程
                // Kotlin object 需要用 .INSTANCE 从 Groovy 获取单例
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                        .use(taskProvider)
                        .toTransform(
                                ScopedArtifact.CLASSES.INSTANCE,
                                { task -> task.getAllJars() },
                                { task -> task.getAllDirectories() },
                                { task -> task.getOutput() }
                        )
            }
        }
    }
}
