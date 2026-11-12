plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.5.0"
}

group = "com.pan"
version = "1.2.6"

repositories {
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/gradle-plugin/") }
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        webstorm("2025.3")  // 或 "2025.2"，根据你想测试的版本

        // Add necessary plugin dependencies for compilation here, example:
        // bundledPlugin("com.intellij.java")
    }

    // Jackson —— IntelliJ 2026+ 不再向插件 classloader 暴露平台内置的 Jackson，
    // 必须显式声明并打包进插件的 lib/ 目录
    implementation("com.fasterxml.jackson.core:jackson-core:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // 纯 JVM 单元测试：不依赖 IntelliJ 测试框架，跑得快
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    // Gradle 9 起需要显式声明 launcher 才能运行 JUnit Platform 测试
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "251"
        }

        changeNotes = """
      Initial version
    """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "21"
    }
    test {
        useJUnitPlatform()
    }
}
