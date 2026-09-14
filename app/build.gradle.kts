import java.io.InputStreamReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ktlint)
}

// 代码风格门禁：ktlintCheck 在 CI 中运行（规则以根目录 .editorconfig 为准）
ktlint {
    version.set("1.5.0")
    android.set(true)
    ignoreFailures.set(false)
}

// 构建配置：app/gradle.properties（模板 gradle.properties.example，真实文件已 gitignore），环境变量优先
val slteProps =
    Properties().apply {
        rootProject.file("app/gradle.properties").takeIf { it.isFile() }?.inputStream()?.use {
            load(InputStreamReader(it, Charsets.UTF_8))
        }
    }

fun slteValue(name: String): String? = System.getenv(name)?.takeIf { it.isNotBlank() }
    ?: slteProps.getProperty(name)?.trim()?.takeIf { it.isNotBlank() }

/** 只接受 https：无协议自动补全，显式 http 拒绝 */
fun slteHttps(raw: String): String? = when {
    raw.startsWith("https://") -> raw
    raw.startsWith("http://") -> null
    else -> "https://$raw"
}

/** 从注入地址提取小写域名，供白名单自动并入 */
fun slteHost(url: String): String? = url
    .removePrefix("https://")
    .substringBefore('/')
    .substringBefore(':')
    .takeIf { it.isNotEmpty() }
    ?.lowercase()

// 应用信息
val slteAppName = slteValue("SLTE_APP_NAME") ?: "SLTE"
val slteApplicationId = slteValue("SLTE_APPLICATION_ID") ?: "com.slte.app"
val slteVersionCode = slteValue("SLTE_VERSION_CODE")?.toIntOrNull() ?: 1
val slteVersionName = slteValue("SLTE_VERSION_NAME") ?: "1.0.0"

// 后端 API
val slteApiBaseUrl = slteValue("SLTE_API_BASE_URL")?.let(::slteHttps) ?: "https://api.example.com"
val slteApiType = slteValue("SLTE_API_TYPE") ?: "xiaov2b"
val slteSubscribePath = slteValue("SLTE_SUBSCRIBE_PATH") ?: "/api/v1/client/subscribe"
val slteRemoteConfigUrls =
    slteValue("SLTE_REMOTE_CONFIG_URLS")
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.mapNotNull(::slteHttps)
        ?.joinToString(",") ?: ""

// 白名单 = 手动追加 + API 域名 + 配置源域名（远程下发的地址只能在这些域内切换）
val slteAllowedDomains =
    buildList {
        slteValue("SLTE_ALLOWED_DOMAINS")
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?.let(::addAll)
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
                "SLTE-$versionName.apk"
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
            storeFile = rootProject.file(slteReleaseStoreFile ?: "release.keystore")
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
                                "禁止使用 debug 签名发布（本地调试请用 assembleDebug）",
                        )
                    }
                }
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
            // JVM 单测中未 mock 的 android.* 调用返回默认值而非抛异常。
            // 这是必需的：被测代码普遍经 AppLog 输出（底层是 android.util.Log），
            // 设为 false 会让任何走到日志分支的用例因 "not mocked" 直接失败。
            // 代价：用例若误用 android.* 且不经过日志路径，会静默拿到默认值——
            // 因此新写单测请断言业务返回值，不要依赖 android 框架行为。
            isReturnDefaultValues = true
        }
    }

    lint {
        // 门禁语义显式化（默认值，但写明以免被误改）：
        // error 阻断构建；warning 不阻断——现存 warning 全是 GradleDependency 版本提示，
        // 升级由 .github/dependabot.yml 提 PR 处理；若升级为 error，构建会随上游发版随机失败。
        abortOnError = true
        checkReleaseBuilds = true
        // 例外清单（含逐条理由）见 app/lint.xml
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

    bundle {
        language {
            // 语言资源不按语言拆分：应用内设置页可热切换语言（LocaleStore），
            // 若随 AAB 拆分下载，切换后取不到未下载语种的资源
            enableSplit = false
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
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

/**
 * R8 存活校验：release 混淆后，`data/remote` 下所有 `@Serializable` DTO 与 Retrofit 接口
 * 必须仍然存在。
 *
 * 为什么需要这条：这些类在代码里**只以泛型实参**出现（`XiaoV2bResponse<XiaoV2bLoginData>`），
 * 而泛型在 JVM 层被擦除，serializer 又是运行时反射查出来的——R8 的静态可达性分析看不到强引用。
 * 一旦 proguard 规则里带了 `allowshrinking`，R8 会把它们整类删掉，接着 Retrofit 接口方法的
 * 泛型签名也退化成 raw `Object`，于是**每个接口方法**在发请求前就抛
 * `Unable to create converter for class java.lang.Object`，界面只显示"请求失败"。
 *
 * 这条检查必须存在，因为**光构建成功拦不住它**：R8 删类不报错，构建是绿的，只有运行时发请求才炸，
 * 且 debug 构建（不混淆）完全正常——历史事故正是这样藏到第一版 release 的。
 */
val verifyReleaseApiSurvivors =
    tasks.register("verifyReleaseApiSurvivors") {
        group = "verification"
        description = "校验 R8 未删除 data/remote 下的 @Serializable DTO 与 Retrofit 接口"

        val sourceDir = layout.projectDirectory.dir("src/main/java/com/slte/app/data/remote")
        val mappingFile = layout.buildDirectory.file("outputs/mapping/release/mapping.txt")

        dependsOn("minifyReleaseWithR8")
        inputs.dir(sourceDir)
        inputs.file(mappingFile)
        // 该任务只做校验、无输出产物，故每次都跑（读两个文件，开销可忽略）
        outputs.upToDateWhen { false }

        // 注意：实现必须内联在 doLast 里。若提到脚本顶层，doLast 会捕获脚本对象，
        // 配置缓存将无法序列化本任务（之前 verifyNativeLibraries 踩过这个坑）。
        doLast {
            val declaration = Regex("""^\s*(?:@\w+[^)]*\)?\s*)*(?:public |internal |private )?(?:data |sealed |abstract |open )*(class|object|interface)\s+([A-Za-z0-9_]+)""")
            val serializableMarker = "@Serializable"
            val retrofitMarker = Regex("""\binterface\s+[A-Za-z0-9_]*Retrofit\b""")

            val expected = sortedSetOf<String>()
            sourceDir.asFile.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
                var pendingSerializable = false
                file.readLines().forEach { line ->
                    val trimmed = line.trim()
                    when {
                        trimmed.isEmpty() -> pendingSerializable = false
                        trimmed.startsWith(serializableMarker) -> pendingSerializable = true
                        trimmed.startsWith("@") -> Unit // 其它注解不重置待定状态
                        trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*") -> Unit
                        else -> {
                            val name = declaration.find(line)?.groupValues?.get(2)
                            if (name != null && (pendingSerializable || retrofitMarker.containsMatchIn(line))) {
                                expected += name
                            }
                            pendingSerializable = false
                        }
                    }
                }
            }
            if (expected.isEmpty()) {
                throw GradleException("未在 ${sourceDir.asFile} 下找到任何 @Serializable DTO / Retrofit 接口，校验脚本可能已失效")
            }

            val mapping = mappingFile.get().asFile
            if (!mapping.isFile) throw GradleException("缺少 R8 mapping 文件：${mapping.absolutePath}")
            val mappingText = mapping.readText()

            val missing =
                expected.filterNot { name ->
                    val pattern =
                        Regex(
                            "^com\\.slte\\.app\\.data\\.remote\\.[A-Za-z0-9_.]*\\." + Regex.escape(name) + " -> ",
                            RegexOption.MULTILINE,
                        )
                    pattern.containsMatchIn(mappingText)
                }
            if (missing.isNotEmpty()) {
                throw GradleException(
                    "R8 删除了 data/remote 下的 ${missing.size} 个类：${missing.joinToString(", ")}\n" +
                        "这些类通过泛型实参 + 反射 serializer 使用，R8 看不到强引用。\n" +
                        "请检查 app/proguard-rules.pro：相关的 -keep 规则**不能**带 allowshrinking，\n" +
                        "否则 Retrofit 会在发请求前抛 Unable to create converter for class java.lang.Object。",
                )
            }
            logger.lifecycle("R8 存活校验通过（${expected.size} 个 @Serializable DTO / Retrofit 接口）")
        }
    }
