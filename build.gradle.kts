import org.gradle.api.JavaVersion
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.process.CommandLineArgumentProvider
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    // IntelliJ Platform Gradle Plugin 2.x
    id("org.jetbrains.intellij.platform") version "2.20.0"
    // Gradle Changelog Plugin
    id("org.jetbrains.changelog") version "2.5.0"
    id("io.freefair.lombok") version "9.8.3"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-repositories-extension.html
repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // 构建基线 = 最低支持版本（方案 D：2025.3）
        intellijIdea(properties("platformVersion"))

        // 捆绑插件依赖，必须使用完整插件 ID
        bundledPlugin("com.intellij.java")
        bundledPlugin("com.intellij.properties")
        bundledPlugin("org.jetbrains.plugins.yaml")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")

        // 2.x 起测试框架必须显式声明
        testFramework(TestFrameworkType.Platform)
    }

    // https://github.com/alibaba/fastjson2
    implementation("com.alibaba.fastjson2:fastjson2:2.0.34")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-collections4
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("org.yaml:snakeyaml:2.0")
    implementation("commons-beanutils:commons-beanutils:1.11.0")
    implementation("jakarta.validation:jakarta.validation-api:3.1.0")
}

// =====================================================================
// JVM Toolchain 配置 —— 整条兼容链必须闭合
// =====================================================================
//
// 两条独立的 JDK 需求，不要混淆：
//
//   ① Gradle 守护进程运行时 JDK
//      - Gradle 9.0.0 最低要求 JDK 25
//      - 由 JAVA_HOME 或 org.gradle.java.home 指定
//      - 这是 Gradle 自己跑起来用的 JVM
//
//   ② 插件编译用的 toolchain（下面配置的就是这个）
//      - sinceBuild=253（2025.3）绑定 JBR 21 → class file 65.0
//      - untilBuild=263.*（2026.3）绑定 JBR 25 → class file 69.0
//      - 插件字节码必须 = sinceBuild 的 JBR（最低兼容基线）
//      - 高版本 JBR 向上兼容低版本字节码（69.0 能跑 65.0）
//      - 所以 toolchain 必须 = 21，不能是 25！
//
// 错误示范：toolchain 25 → 编译出 class file 69.0 → JBR 21 沙箱崩溃
// 正确示范：toolchain 21 → 编译出 class file 65.0 → JBR 21 和 JBR 25 都能跑
//
// 参考：https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html
// =====================================================================

java {
    // 插件字节码版本必须 sinceBuild 对齐（2025.3 → JBR 21 → class file 65.0）
    // 不限制 vendor：Oracle / OpenJDK / Temurin / Zulu 等任意 JDK 21 发行版均可
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    // Kotlin toolchain 与 Java toolchain 保持一致（同一个 JDK 21）
    // java {} 块已声明 toolchain，这里显式对齐避免歧义
    jvmToolchain(21)
}

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-extension.html
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")

        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
            untilBuild = properties("pluginUntilBuild")
        }

        // Get the latest available change notes from the changelog file
        changeNotes = provider {
            with(changelog) {
                renderItem(
                    getOrNull(properties("pluginVersion"))
                        ?: runCatching { getLatest() }.getOrElse { getUnreleased() },
                    Changelog.OutputType.HTML,
                )
            }
        }
    }

    // 插件无 Settings 界面，无需生成可搜索选项索引
    buildSearchableOptions = false

    pluginVerification {
        ides {
            recommended()
        }
    }

    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = System.getenv("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/deployment.html#specifying-a-release-channel
        channels = listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
    }
}

// https://github.com/JetBrains/gradle-changelog-plugin
changelog {
    groups.set(emptyList())
}

// Configure UI tests plugin
// Read more: https://github.com/JetBrains/intellij-ui-test-robot
val runIdeForUiTests by intellijPlatformTesting.runIde.registering {
    task {
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Drobot-server.port=8082",
                "-Dide.mac.message.dialogs.as.sheets=false",
                "-Djb.privacy.policy.text=<!--999.999-->",
                "-Djb.consents.confirmation.enabled=false",
            )
        }
    }
    plugins {
        robotServerPlugin()
    }
}

tasks {
    wrapper {
        gradleVersion = properties("gradleVersion")
    }

    named("publishPlugin") {
        dependsOn("patchChangelog")
    }
}
