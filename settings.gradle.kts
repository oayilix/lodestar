pluginManagement {
    // Resolve the publishable plugin from a local included build during development.
    // 开发阶段通过本地 included build 解析可发布插件。
    includeBuild("lodestar-gradle-plugin")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lodestar"
include(":app")
include(":bm-a")
include(":bm-b")
include(":lodestar-processor")
include(":lodestar-annotations")
include(":lodestar-api")
