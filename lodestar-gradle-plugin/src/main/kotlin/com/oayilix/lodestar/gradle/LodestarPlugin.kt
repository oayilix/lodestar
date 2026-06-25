package com.oayilix.lodestar.gradle

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Wires route aggregation into every Android application variant.
 * 将路由聚合任务接入每个 Android application variant。
 *
 * Library modules only generate local registries through KSP; the application owns the final
 * cross-module aggregate.
 * Library 模块只通过 KSP 生成局部路由表；最终的跨模块聚合由 application 负责。
 */
class LodestarPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Configure lazily so plugin application order does not affect behavior.
        // 延迟配置，避免插件应用顺序影响行为。
        project.pluginManager.withPlugin("com.android.application") {
            val androidComponents = project.extensions
                .getByType(ApplicationAndroidComponentsExtension::class.java)

            androidComponents.onVariants { variant ->
                val generateTask = project.tasks.register(
                    "generateLodestarRoutes${variant.name.replaceFirstChar { it.uppercase() }}",
                    GenerateLodestarMappingTask::class.java
                )

                // ALL includes project classes and dependency classes, which is required for
                // detecting duplicate routes across modules.
                // ALL 同时包含工程与依赖 class，用于检测跨模块重复路由。
                variant.artifacts.forScope(ScopedArtifacts.Scope.ALL)
                    .use(generateTask)
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
