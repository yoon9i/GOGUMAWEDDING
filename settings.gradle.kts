pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://devrepo.kakao.com/nexus/content/groups/public/") }
    }
}

rootProject.name = "GOGUMA"
include(":app")
 