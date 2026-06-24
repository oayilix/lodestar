package com.oayilix.lodestar.gradle

import com.android.build.api.transform.Transform
import com.android.build.gradle.AppExtension
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
            // 注册 Transform
            AppExtension appExtension = project.extensions.getByType(AppExtension)
            Transform transform = new RouterMappingTransform()
            appExtension.registerTransform(transform)
        }
    }
}