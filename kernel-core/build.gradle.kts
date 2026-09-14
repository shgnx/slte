import java.security.MessageDigest

plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.github.kr328.clash.core"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        minSdk = 28
        consumerProguardFiles("consumer-rules.pro")
        ndk {
            // ponytail: 先只出 arm64-v8a，其余 ABI 需要时再加
            abiFilters += listOf("arm64-v8a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
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
    implementation(project(":kernel-common"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
}

/**
 * 校验 jniLibs 下的预编译二进制与 SHA256SUMS 记录一致。
 *
 * libclash.so 是权限最高的预编译产物（VPN/TUN 路径全部经过它），且由人工交叉编译后放回仓库
 * （见同目录 VERSION.md）——没有摘要校验就无法发现产物被替换或损坏。
 * 摘要真源：src/main/jniLibs/arm64-v8a/SHA256SUMS。
 *
 * 挂在 preBuild 上，因此任何依赖本模块的构建（含 app 的 release 打包）都会先校验。
 */
val verifyNativeLibraries =
    tasks.register("verifyNativeLibraries") {
        group = "verification"
        description = "校验 jniLibs 预编译二进制的 SHA-256 与 SHA256SUMS 一致"

        val jniDir = layout.projectDirectory.dir("src/main/jniLibs/arm64-v8a")
        val manifestFile = jniDir.file("SHA256SUMS").asFile
        val marker = layout.buildDirectory.file("verifyNativeLibraries.ok")

        inputs.dir(jniDir)
        outputs.file(marker)

        // 哈希实现内联在 doLast 里：若提到脚本顶层，doLast 会捕获脚本对象，
        // 配置缓存将无法序列化本任务（capture of Gradle script object）。
        doLast {
            fun sha256Of(file: File): String {
                val digest = MessageDigest.getInstance("SHA-256")
                file.inputStream().use { input ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                return digest.digest().joinToString("") { "%02x".format(it) }
            }

            if (!manifestFile.isFile) {
                throw GradleException("缺少 ${jniDir.asFile.name}/SHA256SUMS：预编译内核二进制必须记录 SHA-256")
            }
            val problems = mutableListOf<String>()
            var checked = 0
            manifestFile
                .readLines()
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .forEach { line ->
                    val parts = line.split(Regex("\\s+"), limit = 2)
                    if (parts.size != 2) throw GradleException("SHA256SUMS 行格式错误（应为 `<摘要>  <文件名>`）: $line")
                    val expected = parts[0].lowercase()
                    val name = parts[1].trim().removePrefix("*")
                    val target = File(jniDir.asFile, name)
                    if (!target.isFile) {
                        problems += "$name 缺失"
                        return@forEach
                    }
                    val actual = sha256Of(target)
                    checked++
                    if (actual != expected) problems += "$name 摘要不一致（记录 $expected，实际 $actual）"
                }
            if (checked == 0) throw GradleException("SHA256SUMS 未记录任何文件")
            if (problems.isNotEmpty()) {
                throw GradleException(
                    "预编译内核二进制校验失败：\n" +
                        problems.joinToString("\n") { "  - $it" } +
                        "\n若确已按 VERSION.md 重建内核，请更新 SHA256SUMS。",
                )
            }
            logger.lifecycle("预编译内核二进制校验通过（$checked 个文件）")
            marker.get().asFile.apply { parentFile.mkdirs() }.writeText("$checked files verified\n")
        }
    }

// 任何构建（含 app 的 assembleRelease）都先校验产物，避免被替换的 so 静默进入发布包
tasks.matching { it.name == "preBuild" }.configureEach { dependsOn(verifyNativeLibraries) }
