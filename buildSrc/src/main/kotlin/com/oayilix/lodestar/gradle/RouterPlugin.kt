package com.oayilix.lodestar.gradle

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class RouterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("RouterPlugin, apply from ${project.name}")

        // 判断当前工程是否有 com.android.application
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            val androidComponents = project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                // 注册一个 Task 来生成汇总的 RouterMapping.class
                val taskProvider = project.tasks.register(
                    "generateRouterMapping${variant.name.replaceFirstChar { it.uppercase() }}",
                    GenerateRouterMappingTask::class.java
                )

                // 将 Task 接入 AGP 的构建流程，作用于所有模块的 CLASSES
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(taskProvider)
                    .toTransform(
                        ScopedArtifact.CLASSES,
                        { it.allJars },
                        { it.allDirectories },
                        { it.output }
                    )
            }
        }
    }
}
