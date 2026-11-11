// Gradle 9.0+ 移除 foojay-resolver-convention：
//   根因：Gradle 9 重构了 JvmVendorSpec（移除 IBM_SEMERU 等字段），
//         而 foojay 0.9.0 / 1.0.0 仍引用旧字段导致构建失败
//   替代：Gradle 自动从 PATH / JAVA_HOME / $HOME/.sdkman / /usr/lib/jvm
//         等位置查找 toolchain JDK，无需额外下载器
// 如果系统没有 JDK 21，手动安装一个：
//   sudo apt install openjdk-21-jdk   (Ubuntu)
//   sdk install java 21-temurin       (SDKMAN)
// 然后构建即可
rootProject.name = "Solon"
