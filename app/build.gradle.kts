import java.io.InputStreamReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 构建配置：app/gradle.properties（模板 gradle.properties.example，真实文件已 gitignore），环境变量优先
val slteProps = Properties().apply {
    rootProject.file("app/gradle.properties").takeIf { it.isFile() }?.inputStream()?.use {
        load(InputStreamReader(it, Charsets.UTF_8))
    }
}

fun slteValue(name: String): String? =
    System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: slteProps.getProperty(name)?.trim()?.takeIf { it.isNotBlank() }

/** 只接受 https：无协议自动补全，显式 http 拒绝 */
fun slteHttps(raw: String): String? = when {
    raw.startsWith("https://") -> raw
    raw.startsWith("http://") -> null
    else -> "https://$raw"
}

/** 从注入地址提取小写域名，供白名单自动并入 */
fun slteHost(url: String): String? = url.removePrefix("https://")
    .substringBefore('/').substringBefore(':')
    .takeIf { it.isNotEmpty() }?.lowercase()

// 应用信息
val slteAppName = slteValue("SLTE_APP_NAME") ?: "SLTE"
val slteApplicationId = slteValue("SLTE_APPLICATION_ID") ?: "com.slte.app"
val slteVersionCode = slteValue("SLTE_VERSION_CODE")?.toIntOrNull() ?: 1
val slteVersionName = slteValue("SLTE_VERSION_NAME") ?: "1.0.0"

// 后端 API
val slteApiBaseUrl = slteValue("SLTE_API_BASE_URL")?.let(::slteHttps) ?: "https://api.example.com"
val slteApiType = slteValue("SLTE_API_TYPE") ?: "xiaov2b"
val slteSubscribePath = slteValue("SLTE_SUBSCRIBE_PATH") ?: "/api/v1/client/subscribe"
val slteRemoteConfigUrls = slteValue("SLTE_REMOTE_CONFIG_URLS")
    ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.mapNotNull(::slteHttps)
    ?.joinToString(",") ?: ""

// 白名单 = 手动追加 + API 域名 + 配置源域名（远程下发的地址只能在这些域内切换）
val slteAllowedDomains = buildList {
    slteValue("SLTE_ALLOWED_DOMAINS")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.let(::addAll)
    slteValue("SLTE_API_BASE_URL")?.let(::slteHttps)?.let(::add)
    slteRemoteConfigUrls.split(',').filter { it.isNotEmpty() }.forEach(::add)
}.filter { it.isNotEmpty() }.mapNotNull(::slteHost).distinct().joinToString(",")

// Crisp 客服（编译期默认，运行时由远程配置覆盖）
val slteCrispWebsiteId = slteValue("SLTE_CRISP_WEBSITE_ID") ?: ""
val slteCrispEnabled = (slteValue("SLTE_CRISP_ENABLED") ?: "false").toBoolean()

// 发布签名
val slteReleaseStoreFile = slteValue("SLTE_RELEASE_STORE_FILE")

android {
    namespace = "com.slte.app"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    // 安装包输出名带版本号：SLTE-1.0.0.apk / SLTE-1.0.0-debug.apk
    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName =
                "SLTE-${versionName}.apk"
        }
    }

    defaultConfig {
        applicationId = slteApplicationId
        minSdk = 28
        targetSdk = 36
        versionCode = slteVersionCode
        versionName = slteVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // 仅发布 arm64-v8a：内核 so 只有 arm64，其他 ABI 打包会导致安装成功但运行崩溃
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        // 应用显示名（覆盖 strings.xml 的 app_name；图标需自行替换 res/mipmap）
        resValue("string", "app_name", slteAppName)

        // 后端 API 与订阅路径
        buildConfigField("String", "API_BASE_URL", "\"$slteApiBaseUrl\"")
        buildConfigField("String", "API_TYPE", "\"$slteApiType\"")
        buildConfigField("String", "SUBSCRIBE_PATH", "\"$slteSubscribePath\"")

        // 远程配置源与域名白名单（白名单同时是凭据发送的安全边界）
        buildConfigField("String", "REMOTE_CONFIG_URLS", "\"$slteRemoteConfigUrls\"")
        buildConfigField("String", "ALLOWED_DOMAINS", "\"$slteAllowedDomains\"")

        // Crisp 客服（运行时由远程配置 crisp_* 字段覆盖）
        buildConfigField("String", "CRISP_WEBSITE_ID", "\"$slteCrispWebsiteId\"")
        buildConfigField("boolean", "CRISP_ENABLED", "$slteCrispEnabled")
    }

    signingConfigs {
        create("release") {
            storeFile = file(slteReleaseStoreFile ?: "release.keystore")
            storePassword = slteValue("SLTE_RELEASE_STORE_PASSWORD").orEmpty()
            keyAlias = slteValue("SLTE_RELEASE_KEY_ALIAS") ?: "slte"
            keyPassword = slteValue("SLTE_RELEASE_KEY_PASSWORD").orEmpty()
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val hasReleaseKey = slteReleaseStoreFile != null
            if (hasReleaseKey) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                // 禁止 release 静默退回 debug 签名（debug 密钥公开，同签名恶意包可覆盖安装）
                gradle.taskGraph.whenReady {
                    if (allTasks.any { it.name.contains("Release") }) {
                        throw GradleException(
                            "release 构建必须设置 SLTE_RELEASE_STORE_FILE/PASSWORD/KEY_ALIAS/KEY_PASSWORD，" +
                                "禁止使用 debug 签名发布（本地调试请用 assembleDebug）"
                        )
                    }
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            // JVM 单测中未 mock 的 android.* 调用返回默认值而非抛异常
            isReturnDefaultValues = true
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.svg)
    implementation(libs.coil.network.okhttp)
    implementation(libs.multiplatform.markdown.renderer.m3)
    implementation(libs.lottie.compose)
    implementation(libs.maxminddb)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.webkit)

    // VPN 内核（mihomo，经 kaild Binder 与 :background 进程通信）
    implementation(project(":kernel-service"))
    implementation(project(":kernel-common"))
    implementation(libs.kaidl.runtime)

    // Crisp 客服 SDK
    implementation(libs.crisp.sdk)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.snakeyaml)
    // 拦截器/配置竞速集成测试的本地假服务器
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
}
