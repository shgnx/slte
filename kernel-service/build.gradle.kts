import java.io.InputStreamReader
import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
}

// 与 app 模块共用 app/gradle.properties 配置源（环境变量优先），此处仅取通知栏两项
val slteProps = Properties().apply {
    rootProject.file("app/gradle.properties").takeIf { it.isFile() }?.inputStream()?.use {
        load(InputStreamReader(it, Charsets.UTF_8))
    }
}

fun slteValue(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: slteProps.getProperty(name)?.trim()?.takeIf { it.isNotBlank() }

val slteNotificationTitle = slteValue("SLTE_NOTIFICATION_TITLE") ?: ""
val slteNotificationTraffic = slteValue("SLTE_NOTIFICATION_TRAFFIC") ?: "true"

android {
    namespace = "com.github.kr328.clash.service"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")

        // 通知栏：标题（空 = 跟随应用名）与流量显示开关
        buildConfigField("String", "NOTIFICATION_TITLE", "\"$slteNotificationTitle\"")
        buildConfigField("boolean", "NOTIFICATION_TRAFFIC", slteNotificationTraffic)
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // IClashManager 接口直接暴露 core 的模型类型，必须 api 透传给上层
    api(project(":kernel-core"))
    implementation(project(":kernel-common"))

    ksp(libs.kaidl)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.kaidl.runtime)
    implementation(libs.rikkax.multiprocess)
}
