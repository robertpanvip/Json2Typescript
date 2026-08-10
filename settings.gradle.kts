pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugins/") }
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "Json2Typescipt"
