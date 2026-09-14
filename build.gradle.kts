// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

// JDK 范围检查:AGP 8.9 要求 JDK ≥ 17;Gradle 8.13 官方支持运行 JDK ≤ 23。
// 用 JDK 24+ 启动时 Gradle 在本脚本加载前就会拒绝(报 unsupported class file version),
// 这里主要拦截 <17 并给出明确提示,避免用户面对 AGP 的原始报错
val jdkMajor = JavaVersion.current().majorVersion.toIntOrNull() ?: 0
if (jdkMajor in 1..16) {
    throw GradleException(
        "SLTE 需要 JDK 17+ 构建(当前为 ${JavaVersion.current()})。" +
            "请安装 JDK 17 或 21 并设置 JAVA_HOME,详见 README「构建」章节"
    )
}
